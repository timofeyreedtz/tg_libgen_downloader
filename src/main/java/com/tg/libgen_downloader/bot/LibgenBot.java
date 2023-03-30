package com.tg.libgen_downloader.bot;


import com.tg.libgen_downloader.config.LibgenBotProps;
import com.tg.libgen_downloader.service.LibgenBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class LibgenBot extends TelegramLongPollingBot {
    private final LibgenBotService service;
    private final LibgenBotProps properties;

    private volatile HashMap<Long, String> userCodes = new HashMap<>();
    @Autowired
    public LibgenBot(LibgenBotService service, LibgenBotProps properties) throws TelegramApiException {
        this.properties = properties;
        this.service = service;
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(this);
    }
    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()){
            long chat_id = update.getMessage().getChatId();
            switch (update.getMessage().getText()) {
                case "/start":
                    sendMessage("Hi! This bot can send you books and articles by their ISBN or DOI. Use /help to get more info.", chat_id);
                    break;
                case "/help":
                    sendMessage("To get book/article use /get to choose category to search from. Then write ISBN (for books) and DOI (for articles).", chat_id);
                    break;
                case "/get":
                    setKeyboard(chat_id);
                    break;
                default:
                    new Thread(()->{
                        if(userCodes.get(update.getMessage().getFrom().getId()) != null){
                            String category = userCodes.get(update.getMessage().getFrom().getId());
                            sendMessage("Bot's searching the book and trying to download it. If something goes wrong you'll get a message.",chat_id);
                            String response =  service.getUrl(update.getMessage().getText(),category);
                            responseHandler(response,chat_id);
                            userCodes.remove(update.getMessage().getFrom().getId());
                        }
                        else{
                            sendMessage("You haven't chosen category. Use /get to choose",chat_id);
                        }
                    }).start();
            }
        }
        else if(update.hasCallbackQuery()){
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            switch ((update.getCallbackQuery().getData())){
                case "SCIENCE":
                    sendMessage("Enter book's name",chat_id);
                    userCodes.put(update.getCallbackQuery().getFrom().getId(),"SCIENCE");
                    break;
                case"FICTION":
                    sendMessage("Enter book's name",chat_id);
                    userCodes.put(update.getCallbackQuery().getFrom().getId(),"FICTION");
                    break;
                case "SCIENCE_ARTICLE":
                    sendMessage("Enter book's name",chat_id);
                    userCodes.put(update.getCallbackQuery().getFrom().getId(),"SCIENCE_ARTICLE");
                    break;
            }
        }
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }
    private void sendMessage(String message,long chat_id){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat_id);
        sendMessage.setText(message);
        try{
            execute(sendMessage);
        }
        catch (TelegramApiException exception){

        }
    }
    private void sendFile(String path,long chatId){
        InputFile inputFile = new InputFile(new File(path));
        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(chatId);
        sendDocumentRequest.setDocument(inputFile);
        try {
            execute(sendDocumentRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void responseHandler(String response,long chatId){
        if(response.contains(".pdf")
                ||response.contains(".djvu")
                ||response.contains(".epub")
                ||response.contains(".zip")
                ||response.contains(".rar")){
            sendFile(response,chatId);
        }
        else{
            sendMessage(response,chatId);
        }
    }
    private void setKeyboard(long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Choose category, where you want to find book");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var scienceButton = new InlineKeyboardButton();

        scienceButton.setText("Science / non-fiction");
        scienceButton.setCallbackData("SCIENCE");

        var fictionButton = new InlineKeyboardButton();

        fictionButton.setText("Fiction");
        fictionButton.setCallbackData("FICTION");

        var science_articleButton = new InlineKeyboardButton();

        science_articleButton.setText("Scientific article");
        science_articleButton.setCallbackData("SCIENCE_ARTICLE");

        rowInLine.add(scienceButton);
        rowInLine.add(fictionButton);
        rowInLine.add(science_articleButton);
        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
