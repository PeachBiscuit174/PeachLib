package de.peachbiscuit174.peachlib.data.time;

import org.jetbrains.annotations.ApiStatus;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Provides a highly accurate timestamp by synchronizing with an external time server.
 * Utilizes true NTP (Network Time Protocol) for highest accuracy, including RTT (Round-Trip Time) compensation.
 * Gracefully falls back to HTTP headers if UDP port 123 is blocked by a firewall.
 */
@ApiStatus.Internal
public class TimeProvider {

    private volatile long timeOffset = 0L;
    private volatile boolean synchronizedSuccessfully = false;

    /**
     * Initiates asynchronous time synchronization.
     * Tries NTP first, falls back to HTTP, and defaults to local system time on total failure.
     */
    public void syncAsync() {
        CompletableFuture.runAsync(this::executeSync);
    }

    /**
     * Blocks the current thread to synchronize the time.
     * Essential for server startup where time accuracy is critical immediately.
     */
    public void syncBlocking() {
        executeSync();
    }

    private void executeSync() {
        try {
            // Primary Strategy: Attempt true NTP synchronization (Gold Standard)
            syncNtp();
        } catch (Exception ntpException) {
            // Secondary Strategy: Fallback to HTTP Header Date (Firewall-friendly)
            try {
                syncHttp();
            } catch (Exception httpException) {
                // Total failure: Fallback to local system time
                this.timeOffset = 0L;
                this.synchronizedSuccessfully = false;
            }
        }
    }

    /**
     * Connects to a public NTP pool via UDP to calculate a highly accurate time offset.
     * Includes RTT (Round-Trip Time) compensation for exact latency adjustment.
     *
     * @throws Exception If the network is unreachable or port 123 is blocked.
     */
    private void syncNtp() throws Exception {
        String ntpServer = "pool.ntp.org";

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000); // 3-second timeout

            byte[] buffer = new byte[48];
            buffer[0] = 0x1B; // NTP Mode 3 (Client), Version 3

            InetAddress address = InetAddress.getByName(ntpServer);
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, 123);

            // Start stopwatch
            long requestTime = System.currentTimeMillis();
            socket.send(request);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            // Stop stopwatch
            long responseTime = System.currentTimeMillis();

            // Extract the seconds part from the transmit timestamp (bytes 40-43)
            long seconds = 0;
            for (int i = 40; i <= 43; i++) {
                seconds = (seconds << 8) | (buffer[i] & 0xFF);
            }

            // Extract the fractional part (bytes 44-47) for higher precision
            long fraction = 0;
            for (int i = 44; i <= 47; i++) {
                fraction = (fraction << 8) | (buffer[i] & 0xFF);
            }

            // NTP uses an epoch starting in 1900. Java uses 1970. We must offset the difference.
            long epochOffset = 2208988800L;
            long rawNetworkTime = ((seconds - epochOffset) * 1000) + ((fraction * 1000L) / 0x100000000L);

            // Exact calculation: Add half of the Round-Trip Time to the received time
            long rtt = responseTime - requestTime;
            long exactNetworkTime = rawNetworkTime + (rtt / 2);

            this.timeOffset = exactNetworkTime - responseTime;
            this.synchronizedSuccessfully = true;
        }
    }

    /**
     * Connects via HTTP to read the Date header as a firewall-safe fallback.
     * Includes basic RTT compensation.
     *
     * @throws Exception If the network is unreachable.
     */
    private void syncHttp() throws Exception {
        URL url = new URL("https://google.com");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);

        long requestTime = System.currentTimeMillis();
        connection.connect();
        long networkTime = connection.getDate();
        long responseTime = System.currentTimeMillis();

        connection.disconnect();

        if (networkTime <= 0) {
            throw new IllegalStateException("Invalid HTTP date header.");
        }

        long rtt = responseTime - requestTime;
        long exactNetworkTime = networkTime + (rtt / 2);

        this.timeOffset = exactNetworkTime - responseTime;
        this.synchronizedSuccessfully = true;
    }

    /**
     * Retrieves the current, most accurate timestamp available.
     *
     * @return Current timestamp in milliseconds.
     */
    public long getCurrentTime() {
        return System.currentTimeMillis() + timeOffset;
    }

    /**
     * Verifies if the time provider successfully synced with an external server.
     *
     * @return True if synced externally, false if relying strictly on local system time.
     */
    public boolean isSynchronized() {
        return synchronizedSuccessfully;
    }
}