package game.com;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends ListenerAdapter {

    // قائمة الكلمات الفصيحة مصلحة برمجياً وموزعة بشكل سليم
    private final List<String> wordPool = Arrays.asList(
        "رأي", "فكر", "ذكاء", "المهند", "القرطاس", "المستغفرون",
        "ذات", "نفس", "شهاب", "الصنديد", "الأخدود", "فاستعصم",
        "حب", "ود", "طيبة", "الحسام", "اليعقوب", "أفلمحموها",
        "أهل", "دار", "رحابة", "الهيدب", "السرادق", "المستعبرون",
        "خلق", "طهر", "عفة", "المشرفي", "العقود", "المستقرون",
        "فضل", "خير", "لباقة", "الفارس", "الخيالة", "المستغفرون",
        "حزم", "عزم", "شجاعة", "الهطل", "الصمصام", "فاستوقدها",
        "سعد", "فرح", "بشر", "الغيظ", "الغسق", "المستنطقون",
        "حجر", "صخر", "صلابة", "العسجد", "العثير", "أفأبصرتم",
        "زين", "ثوب", "أناقة", "الريحان", "القرنفل", "فاستنصروه",
        "فهم", "عقل", "فطنة", "القيروان", "الشنبرة", "المستشهدون",
        "زمن", "وقت", "ديمومة", "الأزل", "السرمد", "فاستبرقوا",
        "رسم", "وجه", "وسامة", "اليعفور", "اليعاقيب", "فأنشؤوها",
        "شد", "ركض", "سرعة", "الأنفاق", "السراديب", "المستعجلون",
        "راد", "رزق", "قناعة", "الغيل", "اليعسوب", "فاستطعمهم",
        "صحبة", "ثقة", "بشاشة", "الصدا", "الصداد", "المستعتبون"
    );

    private boolean isGameActive = false;
    private String currentTargetWords = "";
    private long gameStartTime = 0;
    private ScheduledExecutorService timerExecutor;
    private String gameChannelId = ""; 

    // لوحة حفظ النقاط
    private final Map<String, Integer> scoreBoard = new HashMap<>();

    public static void main(String[] args) {
        
        // التوكن السري الخاص ببوتك تم تثبيته هنا
        String token = System.getenv("DISCORD_TOKEN");

        try {
            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new Main())
                    .build();
            System.out.println("✅ تم تشغيل البوت بنجاح من Eclipse! وهو الآن أونلاين.");
        } catch (Exception e) {
            System.out.println("❌ خطأ في التشغيل: " + e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
    	 //   if (event.getAuthor().isBot()) return;

        String messageContent = event.getMessage().getContentRaw().trim();
        String userId = event.getAuthor().getId();
        String userMention = event.getAuthor().getAsMention();

        // 1. أمر بدء اللعبة (!مقال)
        if (messageContent.equals("!مقال") && !isGameActive) { {
            if (isGameActive) {
                event.getChannel().sendMessage("⚠️ فيه لعبة شغالة الحين! انتظر لين تنتهي.").queue();
                return;
            }

            isGameActive = true;
            gameChannelId = event.getChannel().getId();
            currentTargetWords = generateRandomWords(6);

            event.getChannel().sendMessage("🎮 **بدأت لعبة مقال!**\n" +
                    "أسرع واحد يكتب الكلمات التالية بنفس الترتيب وبدون أخطاء يفوز:\n\n" +
                    "📝 **` " + currentTargetWords + " `**\n\n" +
                    "⏱️ معكم **30 ثانية** فقط! انطلقواا💨").queue();

            gameStartTime = System.currentTimeMillis();

            timerExecutor = Executors.newSingleThreadScheduledExecutor();
            timerExecutor.schedule(() -> {
                if (isGameActive && event.getChannel().getId().equals(gameChannelId)) {
                    isGameActive = false;
                    event.getChannel().sendMessage("⏱️ **انتهى الوقت (30 ثانية)!** محد كتب الكلمات صح وبسرعة. حظ أوفر الجولة الجاية! 💥").queue();
                }
            }, 30, TimeUnit.SECONDS);

            return;
        }

        // 2. أمر عرض النقاط (!نقاط)
        if (messageContent.equalsIgnoreCase("!نقاط")) {
            if (scoreBoard.isEmpty()) {
                event.getChannel().sendMessage("📊 لوحة الصدارة فاضية الحين، ما فيه أحد عنده نقاط.").queue();
                return;
            }
            StringBuilder sb = new StringBuilder("📊 **لوحة صدارة لعبة مقال:**\n");
            for (Map.Entry<String, Integer> entry : scoreBoard.entrySet()) {
                sb.append("<@").append(entry.getKey()).append("> : ").append(entry.getValue()).append(" نقطة 🏆\n");
            }
            event.getChannel().sendMessage(sb.toString()).queue();
            return;
        }

        // 3. التحقق من إجابة اللاعب السريعة
        if (isGameActive && event.getChannel().getId().equals(gameChannelId)) {
            if (messageContent.equals(currentTargetWords)) {
                isGameActive = false;
                if (timerExecutor != null && !timerExecutor.isShutdown()) {
                    timerExecutor.shutdownNow();
                }

                long timeTakenMillis = System.currentTimeMillis() - gameStartTime;
                double timeTakenSeconds = timeTakenMillis / 1000.0;

                scoreBoard.put(userId, scoreBoard.getOrDefault(userId, 0) + 1);
                int totalPoints = scoreBoard.get(userId);

                event.getChannel().sendMessage("🎉 **فوز كااااسح!** " + userMention + " هو الأسرع!\n" +
                        "⏱️ الوقت المستغرق: **" + timeTakenSeconds + "** ثانية.\n" +
                        "🏅 رصيدك الحالي: **" + totalPoints + "** نقطة.").queue();
            }
        }
    }

    private String generateRandomWords(int count) {
        List<String> shuffled = new ArrayList<>(wordPool);
        Collections.shuffle(shuffled);
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(shuffled.get(i));
            if (i < count - 1) {
                result.append(" - ");
            }
        }
        return result.toString();
    }
}
