package com.github.kuramastone.cobblemonChallenges.utils;

import com.mojang.datafixers.util.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static boolean doesStringContainCategory(String[] searchList, String actual) {

        actual = actual.replaceAll("/", "");

        boolean doesStringContainCategory = false;
        for (String enemyCategory : searchList) {
            if (enemyCategory.equalsIgnoreCase("any") ||
                    enemyCategory.equalsIgnoreCase(actual)) {
                doesStringContainCategory = true;
                break;
            }
        }
        return doesStringContainCategory;
    }

    public static String parseProgressBar(String input, double progress) {

        // Define the regex pattern
        Pattern pattern = Pattern.compile("\\{progress_bar_(\\d+):([^\\}:]+):([^}]+)\\}");
        Matcher matcher = pattern.matcher(input);

        // Check if the pattern matches and extract the groups
        if (matcher.find()) {
            String value1 = matcher.group(1);  // "15"
            String value2 = matcher.group(2);  // "2"
            String value3 = matcher.group(3);  // "&r|"
            String bar = StringUtils.buildProgressBar(progress, Integer.valueOf(value1), value2, value3);

            return input.replaceAll(pattern.pattern(), bar);
        }

        return input;
    }

    public static String buildProgressBar(double percentage, int barLength, String insertColor, String fillCharacter) {
        int currentProgress = (int) Math.floor(percentage * barLength);

        if (currentProgress < 0)
            currentProgress = 0;
        if (currentProgress > barLength)
            currentProgress = barLength;

        String string = repeat(fillCharacter, barLength);

        String before = string.substring(0, currentProgress);
        String after = string.substring(currentProgress);

        return before + insertColor + after;
    }

    public static String repeat(String str, int times) {
        if(times == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(str);
        }
        return builder.toString();
    }

    public static long stringToMilliseconds(String time) {
        if (time.equals("-1")) {
            return -1;
        }
        long totalMilliseconds = 0;

        // Define conversion factors
        long millisPerSecond = 1000;
        long millisPerMinute = millisPerSecond * 60;
        long millisPerHour = millisPerMinute * 60;
        long millisPerDay = millisPerHour * 24;
        long millisPerWeek = millisPerDay * 7;

        // Regular expression to capture each time unit
        Pattern pattern = Pattern.compile("(\\d+)w|(\\d+)d|(\\d+)h|(\\d+)m|(\\d+)s");
        Matcher matcher = pattern.matcher(time);

        // Parse the input string and calculate total milliseconds
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Weeks
                totalMilliseconds += Long.parseLong(matcher.group(1)) * millisPerWeek;
            }
            else if (matcher.group(2) != null) {
                // Days
                totalMilliseconds += Long.parseLong(matcher.group(2)) * millisPerDay;
            }
            else if (matcher.group(3) != null) {
                // Hours
                totalMilliseconds += Long.parseLong(matcher.group(3)) * millisPerHour;
            }
            else if (matcher.group(4) != null) {
                // Minutes
                totalMilliseconds += Long.parseLong(matcher.group(4)) * millisPerMinute;
            }
            else if (matcher.group(5) != null) {
                // Seconds
                totalMilliseconds += Long.parseLong(matcher.group(5)) * millisPerSecond;
            }
        }

        return totalMilliseconds;

    }

    public static String collapseWithNextLines(Collection<String> stringList) {
        StringBuilder sb = new StringBuilder();

        int index = -1;
        for (String s : stringList) {
            index++;
            sb.append(s);
            if (index != stringList.size() - 1) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    public static List<String> appendNextLinesFromList(List<String> lore) {
        // convert "\n" into a new entry in the list
        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            // Splitting the line at each "\n" character
            String[] parts = StringUtils.splitByLineBreak(line);
            newLore.addAll(Arrays.asList(parts));
        }

        return newLore;
    }

    public static String[] splitByLineBreak(String line) {
        if (line == null || line.isEmpty()) {
            return new String[0]; // Return an empty array if input is null or empty
        }

        // just to be safe...
        return line.split("\\r\\n|\\r|\\n|\\u2028|\\u2029|\n");
    }

    public static String formatSecondsToString(long seconds) {
        // Check if the time is more than or equal to one day
        if (seconds >= 86400) { // 86400 seconds = 1 day
            long days = TimeUnit.SECONDS.toDays(seconds);
            long remainingSeconds = seconds % 86400; // Seconds left after the days part

            if (remainingSeconds == 0) {

                if(days == 1) {
                    return "24 hours";
                }
                else {
                    return days + " days"; // Full day duration
                }

            } else {
                return days + (days == 1 ? " day " : " days ") + formatHHMMSS(remainingSeconds);
            }
        } else {
            // For less than a day, just return in HH:mm:ss format
            return formatHHMMSS(seconds);
        }
    }

    // Helper method to format time as HH:mm:ss
    private static String formatHHMMSS(long seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public static void main(String[] args) {
        String regex = "(&[a-fr0-9klmno])?([^&]*)";
        String input = "       &2Hello!";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        List<Pair<String, String>> result = new ArrayList<>();

        StringBuilder combinedColorCodes = new StringBuilder();

        while (matcher.find()) {
            // matcher.group(0) is the entire string
            // matcher.group(1) is the color code (with the '&')
            // matcher.group(2) is the text

            System.out.println("g0:"+matcher.group(0));
            System.out.println("g1:"+matcher.group(1));
            System.out.println("g2:"+matcher.group(2));

            // ignore empty text
            if(matcher.group(0).isEmpty() && (matcher.group(2) == null || matcher.group(2).isEmpty())) {
                continue;
            }

            // if the string doesnt start with a color, it would trim the start. This manually inserts it even in that scenario
            if(matcher.group(0).isBlank()) {
                result.add(Pair.of("z", matcher.group(0)));
                continue;
            }

            // Remove the '&' from the color code
            String colorCode = matcher.group(1).substring(1);

            // If there is an ongoing color code, concatenate it with the current one
            combinedColorCodes.append(colorCode);

            // If there's text, store the combined color code and the text
            if (!matcher.group(2).isEmpty()) {
                result.add(Pair.of(combinedColorCodes.toString(), matcher.group(2)));
                combinedColorCodes = new StringBuilder(); // Reset the color code for the next match
            }
        }

        for (Pair<String, String> pair : result) {
            System.out.println("%s:'%s'".formatted(pair.getFirst(), pair.getSecond()));
        }
    }

    public static List<String> centerStringListTags(List<String> lore) {
        List<String> list = new ArrayList<>(lore.size());

        int max_length = 0;
        for(String string : lore) {
            if(!string.startsWith("<center>")) {
                max_length = Math.max(max_length, string.length());
            }
        }

        for (int i = 0; i < lore.size(); i++) {
            String newLine = lore.get(i);

            StringBuilder out = new StringBuilder();
            if(newLine.startsWith("<center>")) {
                newLine = newLine.substring("<center>".length()); // trim off center
                int spaceCount = Math.max(0, (max_length / 2) - (newLine.length() / 2));
                out.append(StringUtils.repeat(" ", spaceCount)).append(newLine);
            }
            else {
                out.append(newLine);
            }

            list.add(out.toString());
        }

        return list;
    }

    public static boolean doesListMeetRequiredList(List<String> requiredLabels, Set<String> labels) {
        if(requiredLabels.isEmpty())
            return true;
        if(requiredLabels.stream().anyMatch(it -> it.equalsIgnoreCase("any")))
            return true;

        // check that all required labels exist inside the labels set
        return requiredLabels.stream().allMatch(it -> labels.stream().anyMatch(it::equalsIgnoreCase));
    }

    public static boolean doesListMeetRequiredList(String[] requiredLabels, Set<String> labels) {
        return doesListMeetRequiredList(Arrays.asList(requiredLabels), labels);
    }
}
