package com.example.demoapp.chat;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import com.example.demoapp.utils.UUIDHelper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionFormatter {
    
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s@]+|\"[^\"]+\"|'[^']+'|[\\u4e00-\\u9fa5]+\\d*)");
    
    /**
     * 在已有的 Spannable 上应用 mention 格式化，保留原有的 spans（如链接、图片等）
     */
    public static CharSequence formatMentionsOnSpannable(Spannable spannable) {
        String text = spannable.toString();
        
        String currentUuid = UUIDHelper.getUUID();
        String currentUsername = "User" + currentUuid;
        
        Matcher matcher = MENTION_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String mention = matcher.group(1);
            if (mention != null) {
                String cleanMention = mention.replaceAll("^[\"']|[\"']$", "");
                
                boolean isSelfMention = isCurrentUser(cleanMention, currentUuid, currentUsername);
                
                int start = matcher.start();
                int end = matcher.end();
                
                if (isSelfMention) {
                    // Self mention - yellow/orange highlight
                    spannable.setSpan(
                            new BackgroundColorSpan(Color.parseColor("#FFF3CD")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    spannable.setSpan(
                            new ForegroundColorSpan(Color.parseColor("#E67E22")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                } else {
                    // Other mention - blue highlight
                    spannable.setSpan(
                            new BackgroundColorSpan(Color.parseColor("#E3F2FD")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    spannable.setSpan(
                            new ForegroundColorSpan(Color.parseColor("#0056B3")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }
        }
        
        return spannable;
    }
    
    public static CharSequence formatMentions(String text) {
        SpannableString spannable = new SpannableString(text);
        
        String currentUuid = UUIDHelper.getUUID();
        String currentUsername = "User" + currentUuid;
        
        Matcher matcher = MENTION_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String mention = matcher.group(1);
            if (mention != null) {
                String cleanMention = mention.replaceAll("^[\"']|[\"']$", "");
                
                boolean isSelfMention = isCurrentUser(cleanMention, currentUuid, currentUsername);
                
                int start = matcher.start();
                int end = matcher.end();
                
                if (isSelfMention) {
                    // Self mention - yellow/orange highlight
                    spannable.setSpan(
                            new BackgroundColorSpan(Color.parseColor("#FFF3CD")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    spannable.setSpan(
                            new ForegroundColorSpan(Color.parseColor("#E67E22")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                } else {
                    // Other mention - blue highlight
                    spannable.setSpan(
                            new BackgroundColorSpan(Color.parseColor("#E3F2FD")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    spannable.setSpan(
                            new ForegroundColorSpan(Color.parseColor("#0056B3")),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }
        }
        
        return spannable;
    }
    
    private static boolean isCurrentUser(String mentionedUsername, String currentUuid, String currentUsername) {
        String[] possibleMatches = {
                currentUuid,
                currentUsername,
                "me",
                "Me",
                "ME",
                "@me"
        };
        
        for (String match : possibleMatches) {
            if (mentionedUsername.equalsIgnoreCase(match)) {
                return true;
            }
        }
        
        return false;
    }
}
