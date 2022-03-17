import com.google.gson.Gson;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MessageListener extends ListenerAdapter {
    private static final int DEFAULT_RANGE = 100;
    private List<Coffee> coffeeList = new ArrayList<>();
    private Paginator.Builder pb;

    private void init(){
        for (File x : new File("").listFiles()){
            coffeeList.add(new Coffee("image_folder", new File("")));
        }
    }

    public static void main(String[] args)
            throws LoginException, RateLimitedException, InterruptedException
    {
        JDA jda = new JDABuilder(AccountType.BOT)
                .setToken("YOUR_TOKEN")
                .buildBlocking();
        jda.addEventListener(new MessageListener());
    }



    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String input = event.getMessage().getContentStripped();
        MessageChannel channel = event.getMessage().getChannel();

        if (!event.getAuthor().isBot()) {
            response(input, channel);
            System.out.println(String.format("Input: %s", input));
        }

    }

    private void response(String input, MessageChannel channel){
        String command = input.substring(1).toLowerCase();
        if (input.startsWith("!")){
            if (command.startsWith("roll")){
                int range = DEFAULT_RANGE;
                if (command.length() > 5 && StringUtils.isNumeric(command.substring(5))) {
                    range = Integer.parseInt(command.substring(5));
                    if (range > 0){
                        channel.sendMessage(String.format("%s", new Random().nextInt(range))).queue();
                    }else{
                        channel.sendMessage("Range must be an integer larger than 0.").queue();
                    }
                }else{
                    channel.sendMessage(String.format("%s", new Random().nextInt(range))).queue();
                }
            }else if (command.startsWith("search")){
                File result = null;
                try {
                    result = search(command.substring(7));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (result != null){
                    channel.sendFile(result).queue();
                }else{
                    channel.sendMessage("No results found :(").queue();
                }
            }else if (command.startsWith("coffee") || command.startsWith("kaffee")){
                //TODO
            }else if (command.startsWith("help")){
                channel.sendMessage("```!roll [limit] - rolls a number from 0 to 100\n" +
                        "!search [query] - fetches an image from google\n" +
                        "!help - this```").queue();
            }
        }
    }


    private MessageAction reply(CharSequence message, MessageChannel channel){
        Message m = new MessageBuilder().append(message).build();
        return channel.sendMessage(m);
    }

    private File search(String query) throws IOException {
        String key = "your_key";
        String qry = query;
        String cx  = "your_cx";
        String fileType = "png,jpg";
        String searchType = "image";
        URL url = new URL ("https://www.googleapis.com/customsearch/v1?key=" +key+ "&cx=" +cx+ "&q=" +qry+"&fileType="+fileType+"&searchType="+searchType+"&alt=json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        BufferedReader br = new BufferedReader(new InputStreamReader( ( conn.getInputStream() ) ) );
        GResults results = new Gson().fromJson(br, GResults.class);
        conn.disconnect();

        File result = null;
        for (int i=0; i < 10; i++) {
            String path = "";
            try {
                path  = results.getLink(i);
            }catch(NullPointerException e){
                break;
            }
            File file = new File(String.format(
                    "%s/%s",
                    System.getProperty("user.dir"),
                    path.substring(path.lastIndexOf("/")+1)
            ));

            try {
                FileUtils.copyURLToFile(
                        new URL(path),
                        file,
                        1000,
                        1000
                );
            } catch (IOException e) {
                System.err.println("Something went wrong with the image.");
            }

            if (file.length() < Message.MAX_FILE_SIZE
                    && (file.getName()
                    .substring(file.getName().lastIndexOf('.')+1)
                    .equalsIgnoreCase("jpg")
                    || file.getName()
                    .substring(file.getName().lastIndexOf('.')+1)
                    .equalsIgnoreCase("png")
                    || file.getName()
                    .substring(file.getName().lastIndexOf('.')+1)
                    .equalsIgnoreCase("gif")
                    || file.getName()
                    .substring(file.getName().lastIndexOf('.')+1)
                    .equalsIgnoreCase("jpeg"))){
                result = file;
                break;
            }
        }

        return result;
    }
}