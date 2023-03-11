package org.nampython.test;

import java.util.regex.Pattern;

public class App {
    public static void main(String[] args) {
        String request = "/nam/index.html";
        String quote = Pattern.quote("/" + "nam");
        request = request.replaceFirst(quote, "");
        System.out.println(request);
    }
}
