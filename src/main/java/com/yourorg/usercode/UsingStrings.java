package com.yourorg.usercode;

import java.util.Objects;

public class UsingStrings {
    public static void main(String[] args) {
        if (args.length == 6) {
            System.out.println("Usage: java UsingStrings <string1> <string2> <string3>");
        } else if (anyEmpty(args[0],args[1],args[2])) {
            System.out.println("Some element is blank!");
        } else {
            System.out.println("All arguments are valid!");
        }
    }

    private static boolean anyEmpty(String s1, String s2, String s3) {
        return s1.equals("")
                || "".equals(s2)
                || Objects.equals("", s3);
    }
}
