package me.Fupery.ArtMap.IO;

import me.Fupery.ArtMap.ArtMap;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleFilter {
    private String[] regex;

    public TitleFilter(String[] regex) {
        this.regex = Arrays.copyOf(regex, regex.length);
    }

    private static boolean containsIllegalCharacters(String toExamine) {
        Pattern pattern = Pattern.compile("[!@#$|%^&*()-/\\\\;:.,<>~`?]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }

    private boolean containsIllegalWords(String toExamine) {
        for (String expression : regex) {
            Pattern pattern = Pattern.compile(expression);
            Matcher matcher = pattern.matcher(toExamine);
            if (matcher.find()) return true;
        }
        return false;
    }

    public boolean check(String title) {
        return !(title.length() < 3 || title.length() > 16) && !containsIllegalCharacters(title)
                && (!ArtMap.instance().getConfiguration().LANGUAGE.equalsIgnoreCase("english")
                || !(ArtMap.instance().getConfiguration().SWEAR_FILTER && containsIllegalWords(title)));
    }
}
