package com.mingyuans.ping;


import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yanxq on 17/4/20.
 */
public class AndroidPing {
    private static final String TAG= "AndroidPing";

    private static final String REGX_SUMMARY_LINE = "(\\d)\\s*packets transmitted,\\s*(\\d)\\sreceived,\\s*(.+)% packet loss,\\stime\\s(\\d+)ms";
    private static final String REGX_RRT_LINE = "rtt min/avg/max/mdev\\s*=\\s*(\\d+.\\d+)/(\\d+.\\d+)/(\\d+.\\d+)/(\\d+.\\d+)\\s*ms";

    private static final Pattern PATTERN_SUMMARY = Pattern.compile(REGX_SUMMARY_LINE);
    private static final Pattern PATTERN_RRT = Pattern.compile(REGX_RRT_LINE);


    /**
     * 发起 Android Ping
     * @param hostname 地址
     * @param count 次数
     * @param timeoutSecond 单次 Ping timeout 时间
     * @return PingAnswer
     */
    public static PingAnswer simplePing(String hostname,int count, int timeoutSecond) {
        String simplePingCommand = createSimplePingCommand(hostname,count,timeoutSecond);
        String pingAnswerString = ping(simplePingCommand);
        if (pingAnswerString != null && pingAnswerString.length() > 0) {
            return parsePingAnswerString(pingAnswerString);
        }
        return null;
    }

    /**
     * 创建 ping 命令;
     * @param hostname domain
     * @param count count
     * @param timeoutSecond 单位:秒
     * @return command
     */
    public static String createSimplePingCommand(String hostname, int count, int timeoutSecond) {
        return String.format("/system/bin/ping -c %d -W %d %s",count,timeoutSecond,hostname);
    }

    public static String createPingCommand(String domain, Map<String, String> properties) {
        StringBuilder builder = new StringBuilder("/system/bin/ping");
        if (properties != null && properties.size() > 0) {
            for (Map.Entry<String,String> entry : properties.entrySet()) {
                builder.append(String.format(" -%s %s",entry.getKey(),entry.getValue()));
            }
        }
        builder.append(" " + domain);
        return builder.toString();
    }

    /**
     *
     * 丢包响应:<br><br>
     --- 42.62.69.143 ping statistics --- <br>
     1 packets transmitted, 0 received, 100% packet loss, time 0ms

     * <br><br>
     * 收包响应:<br><br>
     --- 42.62.69.143 ping statistics --- <br>
     1 packets transmitted, 1 received, 0% packet loss, time 0ms <br>
     rtt min/avg/max/mdev = 47.765/47.765/47.765/0.000 ms <br>

     * @param response
     * @return
     */
    public static PingAnswer parsePingAnswerString(String response) {
        if (response == null || response.length() == 0) {
            return null;
        }
        String[] lines = response.split("\n");

        PingAnswer pingAnswer = null;
        boolean isStatisticsLineFound = false;
        for (String line : lines) {
            //find statistics
            if (line == null || (!isStatisticsLineFound && !line.startsWith("---"))) {
                continue;
            }
            if (!isStatisticsLineFound) {
                isStatisticsLineFound = true;
                continue;
            }

            if (line.contains("packets"))  {
                try {
                    Matcher matcher = PATTERN_SUMMARY.matcher(line);
                    if (matcher.find()) {
                        pingAnswer = new PingAnswer();
                        pingAnswer.transmittedPackages = Integer.valueOf(matcher.group(1));
                        pingAnswer.receivedPackages = Integer.valueOf(matcher.group(2));
                        pingAnswer.lossPackagesPercent = Float.valueOf(matcher.group(3));
                    }
                } catch (Throwable throwable) {
                    Log.e(TAG,"getPackageLost: ",throwable);
                }
            } else if (line.contains("rtt")) {
                try {
                    Matcher matcher = PATTERN_RRT.matcher(line);
                    if (matcher.find() && pingAnswer != null) {
                        pingAnswer.rrtMinTimemillis = Float.valueOf(matcher.group(1));
                        pingAnswer.rrtAvgTimemillis = Float.valueOf(matcher.group(2));
                        pingAnswer.rrtMaxTimemillis = Float.valueOf(matcher.group(3));
                    }
                    return pingAnswer;
                } catch (Throwable throwable) {
                    Log.e(TAG,"getRRTTimemillis: ",throwable);
                }
            }
        }
        return pingAnswer;

    }

    public static String ping(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            BufferedReader reader;
            process.waitFor();
            InputStream inputStream = process.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }
            reader.close();
            inputStream.close();
            return builder.toString();
        } catch (Throwable throwable) {
            Log.e(TAG,"Exec ping command with error.",throwable);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return "";
    }

    public static class PingCommandBuilder {
        HashMap<String,String> properties = new HashMap<String, String>();
        String domain = "";
        public PingCommandBuilder(String domain) {
            this.domain = domain;
        }

        /**
         * Ping 的次数
         * @param count count
         * @return PingCommandBuilder
         */
        public PingCommandBuilder count(int count) {
            properties.put("c", String.valueOf(count));
            return this;
        }

        /**
         * 总超时时间, 单位: 秒
         * @param timeout timeout
         * @return
         */
        public PingCommandBuilder timeout(int timeout) {
            properties.put("W", String.valueOf(timeout));
            return this;
        }

        public PingCommandBuilder packageSize(int size) {
            properties.put("s", String.valueOf(size));
            return this;
        }

        public PingCommandBuilder ttl(int ttl) {
            properties.put("t", String.valueOf(ttl));
            return this;
        }

        public String build() {
            return AndroidPing.createPingCommand(domain,properties);
        }
    }

    public static class PingAnswer {
        public int transmittedPackages = 0;
        public int receivedPackages = 0;
        public float lossPackagesPercent = 0;
        public float rrtMinTimemillis = 0;
        public float rrtAvgTimemillis = 0;
        public float rrtMaxTimemillis = 0;

        @Override
        public String toString() {
            return "PingAnswer{" +
                    "transmittedPackages=" + transmittedPackages +
                    ", receivedPackages=" + receivedPackages +
                    ", lossPackagesPercent=" + lossPackagesPercent +
                    ", rrtMinTimemillis=" + rrtMinTimemillis +
                    ", rrtAvgTimemillis=" + rrtAvgTimemillis +
                    ", rrtMaxTimemillis=" + rrtMaxTimemillis +
                    '}';
        }
    }


}
