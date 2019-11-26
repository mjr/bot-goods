import entities.Good;
import entities.GoodsCategory;
import entities.Location;
import entities.Common;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.Commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotGoods extends TelegramLongPollingBot {
    private static final String HELP_TEXT = "I can help you manage Patrimony.\n" +
            "\n" +
            "You can control me by sending these commands:\n" +
            "\n" +
            "/newgood - create a new good\n" +
            "/listgoods - get a list of your goods by location\n" +
            "/searchgood - find your goods by *[code]*, *[name]* or *[description]*\n" +
            "/deletegood - delete an existing good\n" +
            "/movegood - move a good for other location\n" +
            "\n" +
            "*Locations*\n" +
            "/newlocation - create a new location\n" +
            "/listlocations - get a list of your locations\n" +
            "/deletelocation - delete a location\n" +
            "\n" +
            "*Goods Categories*\n" +
            "/newgoodscategory - create a new goods category\n" +
            "/listgoodscategories - get a list of your goods categories\n" +
            "/deletegoodscategory - delete a goods category\n" +
            "\n" +
            "*Reports*\n" +
            "/report - get a report of goods\n" +
            "/reportfile - get a report in *file* of goods";
    private String currentCommand;
    private boolean lastCommandFinished;
    private String lastMessage;
    private Good good;
    private Location location;
    private GoodsCategory goodsCategory;
    private String searchOperation;

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation()) {
                    handleIncomingMessage(message.getChatId(), message.getText(), null);
                }
            } else if (update.hasCallbackQuery()) {
                handleIncomingMessage(
                    update.getCallbackQuery().getMessage().getChatId(),
                    update.getCallbackQuery().getData(),
                    update.getCallbackQuery()
                );
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public ReplyKeyboardMarkup getReplyKeyboardMarkup(List<String> names) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> commands = new ArrayList<>();
        for (String name : names) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add(name);
            commands.add(commandRow);
        }

        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(commands);

        return replyKeyboardMarkup;
    }

    public void newGood(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (good == null) {
            try {
                int code = Integer.parseInt(text);
                if (Good.getByCode(code) != null) {
                    sendMessageRequest.setText("Já existe um bem com este código! Informe um valor de código ainda não utilizado:");
                } else {
                    good = new Good();
                    good.setCode(code);
                    lastMessage = "Informe o nome agora";
                    sendMessageRequest.setText("Informe o nome agora");
                }
            } catch (NumberFormatException e){
                sendMessageRequest.setText("Valor de código invalido! Informe um valor de código válido:");
            }
        } else if (good.getName() == null) {
            good.setName(text);
            lastMessage = "Informe a descrição agora";
            sendMessageRequest.setText("Informe a descrição agora");
        } else if (good.getDescription() == null) {
            good.setDescription(text);
            lastMessage = "Informe a localizacao agora";

            List<String> names = Location.read().stream()
                    .map(Common::getName)
                    .collect(Collectors.toList());

            sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(names));
            sendMessageRequest.setText("Informe a localizacao agora");
        } else if (good.getLocation() == null) {
            Location location = Location.get(text);
            if (location == null) {
                List<String> names = Location.read().stream()
                        .map(Common::getName)
                        .collect(Collectors.toList());

                sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(names));
                sendMessageRequest.setText("Invalid location selected.");
            } else {
                good.setLocation(location);

                lastMessage = "Informe a categoria agora";

                List<String> names = GoodsCategory.read().stream()
                        .map(Common::getName)
                        .collect(Collectors.toList());

                sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(names));
                sendMessageRequest.setText("Informe a categoria agora");
            }
        } else if (good.getCategory() == null) {
            GoodsCategory goodsCategory = GoodsCategory.get(text);
            if (goodsCategory == null) {
                List<String> names = GoodsCategory.read().stream()
                        .map(Common::getName)
                        .collect(Collectors.toList());

                sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(names));
                sendMessageRequest.setText("Invalid category selected.");
            } else {
                good.setCategory(goodsCategory);

                good.save();
                good = null;
                lastCommandFinished = true;
                currentCommand = null;
                lastMessage = null;
                sendMessageRequest.setText("Bem cadastrado com sucesso!");
            }
        }

        execute(sendMessageRequest);
    }

    public void newLocation(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (location == null) {
            try {
                int code = Integer.parseInt(text);
                if (Location.getByCode(code) != null) {
                    sendMessageRequest.setText("Já existe uma localização com este código! Informe um valor de código ainda não utilizado:");
                } else {
                    location = new Location();
                    location.setCode(code);
                    lastMessage = "Informe o nome agora";
                    sendMessageRequest.setText("Informe o nome agora");
                }
            } catch (NumberFormatException e){
                sendMessageRequest.setText("Valor de código invalido! Informe um valor de código válido:");
            }
        } else if (location.getName() == null) {
            location.setName(text);
            lastMessage = "Informe a descrição agora";
            sendMessageRequest.setText("Informe a descrição agora");
        } else if (location.getDescription() == null) {
            location.setDescription(text);
            location.save();
            location = null;
            lastCommandFinished = true;
            currentCommand = null;
            lastMessage = null;
            sendMessageRequest.setText("Localização cadastrada com sucesso!");
        }

        execute(sendMessageRequest);
    }

    public void newGoodsCategory(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (goodsCategory == null) {
            try {
                int code = Integer.parseInt(text);
                if (GoodsCategory.getByCode(code) != null) {
                    sendMessageRequest.setText("Já existe uma categoria com este código! Informe um valor de código ainda não utilizado:");
                } else {
                    goodsCategory = new GoodsCategory();
                    goodsCategory.setCode(code);
                    lastMessage = "Informe o nome agora";
                    sendMessageRequest.setText("Informe o nome agora");
                }
            } catch (NumberFormatException e){
                sendMessageRequest.setText("Valor de código invalido! Informe um valor de código válido:");
            }
        } else if (goodsCategory.getName() == null) {
            goodsCategory.setName(text);
            lastMessage = "Informe a descrição agora";
            sendMessageRequest.setText("Informe a descrição agora");
        } else if (goodsCategory.getDescription() == null) {
            goodsCategory.setDescription(text);
            goodsCategory.save();
            goodsCategory = null;
            lastCommandFinished = true;
            currentCommand = null;
            lastMessage = null;
            sendMessageRequest.setText("Categoria de bem cadastrada com sucesso!");
        }

        execute(sendMessageRequest);
    }

    public void moveGood(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (good == null) {
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(text);
            if (m.find()) {
                good = Good.getByCode(Integer.parseInt(m.group(1)));
                if (good != null) {
                    sendMessageRequest.setText("Please choose a location to move good.");
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                    List<KeyboardRow> commands = new ArrayList<>();
                    List<Location> locations;
                    locations = Location.excludeByCode(good.getLocation().getCode());
                    if (locations.isEmpty()) {
                        lastCommandFinished = true;
                        currentCommand = null;
                        lastMessage = null;
                        good = null;
                        sendMessageRequest.setText("Hmm. This bot has no locations.");
                    } else {
                        for (Location location : locations) {
                            KeyboardRow commandRow = new KeyboardRow();
                            commandRow.add("(" + location.getCode() + ") " + location.getName());
                            commands.add(commandRow);
                        }

                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboard(true);
                        replyKeyboardMarkup.setKeyboard(commands);

                        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                    }
                } else {
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                    List<KeyboardRow> commands = new ArrayList<>();
                    List<Good> goods;
                    goods = Good.read();
                    if (goods.isEmpty()) {
                        lastCommandFinished = true;
                        currentCommand = null;
                        lastMessage = null;
                        good = null;
                        sendMessageRequest.setText("Hmm. This bot has no goods.");
                    } else {
                        for (Good good : goods) {
                            KeyboardRow commandRow = new KeyboardRow();
                            commandRow.add("(" + good.getCode() + ") " + good.getName());
                            commands.add(commandRow);
                        }

                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboard(true);
                        replyKeyboardMarkup.setKeyboard(commands);

                        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                    }
                    sendMessageRequest.setText("Invalid good selected.");
                }
            } else {
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                List<KeyboardRow> commands = new ArrayList<>();
                List<Good> goods;
                goods = Good.read();
                if (goods.isEmpty()) {
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                    good = null;
                    sendMessageRequest.setText("Hmm. This bot has no goods.");
                } else {
                    for (Good good : goods) {
                        KeyboardRow commandRow = new KeyboardRow();
                        commandRow.add("(" + good.getCode() + ") " + good.getName());
                        commands.add(commandRow);
                    }

                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(true);
                    replyKeyboardMarkup.setKeyboard(commands);

                    sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                }
                sendMessageRequest.setText("Invalid good selected.");
            }
        } else {
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(text);
            if (m.find()) {
                location = Location.getByCode(Integer.parseInt(m.group(1)));
                if (location != null) {
                    Location locationPrev = good.getLocation();
                    Good.delete(good.getCode());
                    good.setLocation(location);
                    good.save();
                    sendMessageRequest.setText("Good " + good.getName() + " move of " + locationPrev.getName() + " to " + location.getName());
                } else {
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                    List<KeyboardRow> commands = new ArrayList<>();
                    List<Location> locations;
                    locations = Location.excludeByCode(good.getLocation().getCode());
                    if (locations.isEmpty()) {
                        lastCommandFinished = true;
                        currentCommand = null;
                        lastMessage = null;
                        good = null;
                        sendMessageRequest.setText("Hmm. This bot has no locations.");
                    } else {
                        for (Location location : locations) {
                            KeyboardRow commandRow = new KeyboardRow();
                            commandRow.add("(" + location.getCode() + ") " + location.getName());
                            commands.add(commandRow);
                        }

                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboard(true);
                        replyKeyboardMarkup.setKeyboard(commands);

                        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                    }
                    sendMessageRequest.setText("Invalid location selected.");
                }
            } else {
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                List<KeyboardRow> commands = new ArrayList<>();
                List<Location> locations;
                locations = Location.excludeByCode(good.getLocation().getCode());
                if (locations.isEmpty()) {
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                    good = null;
                    sendMessageRequest.setText("Hmm. This bot has no locations.");
                } else {
                    for (Location location : locations) {
                        KeyboardRow commandRow = new KeyboardRow();
                        commandRow.add("(" + location.getCode() + ") " + location.getName());
                        commands.add(commandRow);
                    }

                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(true);
                    replyKeyboardMarkup.setKeyboard(commands);

                    sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                }
                sendMessageRequest.setText("Invalid location selected.");
            }
        }

        execute(sendMessageRequest);
    }

    public void listGoods(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        String message = "Goods:\n\n";
        if (Location.get(text) == null) {
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

            List<KeyboardRow> commands = new ArrayList<>();
            List<Location> locations;
            locations = Location.read();
            for (Location location : locations) {
                KeyboardRow commandRow = new KeyboardRow();
                commandRow.add(location.getName());
                commands.add(commandRow);
            }

            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            replyKeyboardMarkup.setKeyboard(commands);

            sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
            sendMessageRequest.setText("Invalid location selected.");

            execute(sendMessageRequest);
            return;
        } else {
            List<Good> goods = Good.filterByLocation(text);
            for (Good good : goods) {
                message += good.toString() + "\n---\n";
            }
            if (goods.isEmpty()) {
                sendMessageRequest.setText("Hmm. This location has no goods.");
            } else {
                sendMessageRequest.setText(message);
            }
        }

        lastCommandFinished = true;
        currentCommand = null;
        lastMessage = null;

        execute(sendMessageRequest);
    }

    public void searchGood(Long chatId, String text, CallbackQuery callbackquery) throws TelegramApiException {
        if (searchOperation == null && callbackquery != null) {
            EditMessageText editMarkup = new EditMessageText();
            editMarkup.setChatId(chatId);
            editMarkup.setInlineMessageId(callbackquery.getInlineMessageId());
            editMarkup.setMessageId(callbackquery.getMessage().getMessageId());
            if (text.equals("code")) {
                searchOperation = "code";
                editMarkup.setText("OK. Send me the code for search.");
            } else if (text.equals("name")) {
                searchOperation = "name";
                editMarkup.setText("OK. Send me the name for search.");
            } else if (text.equals("description")) {
                searchOperation = "description";
                editMarkup.setText("OK. Send me the description for search.");
            } else {
                editMarkup.setText("Invalid operation selected.");
            }
            execute(editMarkup);
            return;
        }

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (searchOperation.equals("code")) {
            try {
                int code = Integer.parseInt(text);
                Good good = Good.getByCode(code);
                if (good == null) {
                    sendMessageRequest.setText("Não existe um bem com esse código. Envie outro código:\n\n Ou /cancel para cancelar esta operação.");
                } else {
                    sendMessageRequest.setText(good.toString());
                    searchOperation = null;
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                }
            } catch (NumberFormatException e){
                sendMessageRequest.setText("Valor de código invalido! Informe um valor de código válido:");
            }
        } else if (searchOperation.equals("name")) {
            String message = "Goods:\n\n";
            List<Good> goods = Good.filterByName(text);
            for (Good good : goods) {
                message += good.toString() + "\n---\n";
            }
            if (goods.isEmpty()) {
                sendMessageRequest.setText("Hmm. Não achei nenhum bem com esse nome. Envie outro nome:\n\n Ou /cancel para cancelar esta operação.");
            } else {
                sendMessageRequest.setText(message);
                searchOperation = null;
                lastCommandFinished = true;
                currentCommand = null;
                lastMessage = null;
            }
        } else if (searchOperation.equals("description")) {
            String message = "Goods:\n\n";
            List<Good> goods = Good.filterByDescription(text);
            for (Good good : goods) {
                message += good.toString() + "\n---\n";
            }
            if (goods.isEmpty()) {
                sendMessageRequest.setText("Hmm. Não achei nenhum bem com essa descrição. Envie outra descrição:\n\n Ou /cancel para cancelar esta operação.");
            } else {
                sendMessageRequest.setText(message);
                searchOperation = null;
                lastCommandFinished = true;
                currentCommand = null;
                lastMessage = null;
            }
        } else {
            sendMessageRequest.setText("Invalid operation.");
        }

        execute(sendMessageRequest);
    }

    public void deleteGoodsCategory(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (goodsCategory == null) {
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(text);
            if (m.find()) {
                goodsCategory = GoodsCategory.getByCode(Integer.parseInt(m.group(1)));
                if (goodsCategory != null) {
                    sendMessageRequest.setText("OK, you selected " + goodsCategory.getName() +". Are you sure?\n" +
                            "\n" +
                            "Send 'Yes, I am totally sure.' to confirm you really want to delete this bot.");
                } else {
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                    List<KeyboardRow> commands = new ArrayList<>();
                    List<GoodsCategory> goodsCategories;
                    goodsCategories = GoodsCategory.filterByNull();
                    if (goodsCategories.isEmpty()) {
                        lastCommandFinished = true;
                        currentCommand = null;
                        lastMessage = null;
                        goodsCategory = null;
                        sendMessageRequest.setText("Hmm. This bot has no goods.");
                    } else {
                        for (GoodsCategory goodsCategory : goodsCategories) {
                            KeyboardRow commandRow = new KeyboardRow();
                            commandRow.add("(" + goodsCategory.getCode() + ") " + goodsCategory.getName());
                            commands.add(commandRow);
                        }

                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboard(true);
                        replyKeyboardMarkup.setKeyboard(commands);

                        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                    }
                    sendMessageRequest.setText("Invalid goods category selected.");
                }
            } else {
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                List<KeyboardRow> commands = new ArrayList<>();
                List<GoodsCategory> goodsCategories;
                goodsCategories = GoodsCategory.filterByNull();
                if (goodsCategories.isEmpty()) {
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                    goodsCategory = null;
                    sendMessageRequest.setText("Hmm. This bot has no goods.");
                } else {
                    for (GoodsCategory goodsCategory : goodsCategories) {
                        KeyboardRow commandRow = new KeyboardRow();
                        commandRow.add("(" + goodsCategory.getCode() + ") " + goodsCategory.getName());
                        commands.add(commandRow);
                    }

                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(true);
                    replyKeyboardMarkup.setKeyboard(commands);

                    sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                }
                sendMessageRequest.setText("Invalid goods category selected.");
            }
        } else {
            if (text.equals("Yes, I am totally sure.")) {
                GoodsCategory.delete(goodsCategory.getCode());
                lastCommandFinished = true;
                currentCommand = null;
                lastMessage = null;
                goodsCategory = null;
                sendMessageRequest.setText("Done! The goods category is gone. " + Commands.HELP_COMMAND);
            } else {
                sendMessageRequest.setText("Please enter the confirmation text exactly like this:\n" +
                        "Yes, I am totally sure.\n" +
                        "\n" +
                        "Type /cancel to cancel the operation.");
            }
        }

        execute(sendMessageRequest);
    }

    public void deleteLocation(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (location == null) {
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(text);
            if (m.find()) {
                location = Location.getByCode(Integer.parseInt(m.group(1)));
                if (location != null) {
                    sendMessageRequest.setText("OK, you selected " + location.getName() +". Are you sure?\n" +
                            "\n" +
                            "Send 'Yes, I am totally sure.' to confirm you really want to delete this bot.");
                } else {
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                    List<KeyboardRow> commands = new ArrayList<>();
                    List<Location> locations;
                    locations = Location.filterByNull();
                    if (locations.isEmpty()) {
                        lastCommandFinished = true;
                        currentCommand = null;
                        lastMessage = null;
                        location = null;
                        sendMessageRequest.setText("Hmm. This bot has no locations.");
                    } else {
                        for (Location location : locations) {
                            KeyboardRow commandRow = new KeyboardRow();
                            commandRow.add("(" + location.getCode() + ") " + location.getName());
                            commands.add(commandRow);
                        }

                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboard(true);
                        replyKeyboardMarkup.setKeyboard(commands);

                        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                    }
                    sendMessageRequest.setText("Invalid location selected.");
                }
            } else {
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                List<KeyboardRow> commands = new ArrayList<>();
                List<Location> locations;
                locations = Location.filterByNull();
                if (locations.isEmpty()) {
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                    location = null;
                    sendMessageRequest.setText("Hmm. This bot has no locations.");
                } else {
                    for (Location location : locations) {
                        KeyboardRow commandRow = new KeyboardRow();
                        commandRow.add("(" + location.getCode() + ") " + location.getName());
                        commands.add(commandRow);
                    }

                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(true);
                    replyKeyboardMarkup.setKeyboard(commands);

                    sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                }
                sendMessageRequest.setText("Invalid location selected.");
            }
        } else {
            if (text.equals("Yes, I am totally sure.")) {
                Location.delete(location.getCode());
                lastCommandFinished = true;
                currentCommand = null;
                lastMessage = null;
                location = null;
                sendMessageRequest.setText("Done! The location is gone. " + Commands.HELP_COMMAND);
            } else {
                sendMessageRequest.setText("Please enter the confirmation text exactly like this:\n" +
                        "Yes, I am totally sure.\n" +
                        "\n" +
                        "Type /cancel to cancel the operation.");
            }
        }

        execute(sendMessageRequest);
    }

    public void deleteGood(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        if (good == null) {
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(text);
            if (m.find()) {
                good = Good.getByCode(Integer.parseInt(m.group(1)));
                if (good != null) {
                    sendMessageRequest.setText("OK, you selected " + good.getName() +". Are you sure?\n" +
                            "\n" +
                            "Send 'Yes, I am totally sure.' to confirm you really want to delete this bot.");
                } else {
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                    List<KeyboardRow> commands = new ArrayList<>();
                    List<Good> goods;
                    goods = Good.read();
                    if (goods.isEmpty()) {
                        lastCommandFinished = true;
                        currentCommand = null;
                        lastMessage = null;
                        good = null;
                        sendMessageRequest.setText("Hmm. This bot has no goods.");
                    } else {
                        for (Good good : goods) {
                            KeyboardRow commandRow = new KeyboardRow();
                            commandRow.add("(" + good.getCode() + ") " + good.getName());
                            commands.add(commandRow);
                        }

                        replyKeyboardMarkup.setResizeKeyboard(true);
                        replyKeyboardMarkup.setOneTimeKeyboard(true);
                        replyKeyboardMarkup.setKeyboard(commands);

                        sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                    }
                    sendMessageRequest.setText("Invalid good selected.");
                }
            } else {
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

                List<KeyboardRow> commands = new ArrayList<>();
                List<Good> goods;
                goods = Good.read();
                if (goods.isEmpty()) {
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                    good = null;
                    sendMessageRequest.setText("Hmm. This bot has no goods.");
                } else {
                    for (Good good : goods) {
                        KeyboardRow commandRow = new KeyboardRow();
                        commandRow.add("(" + good.getCode() + ") " + good.getName());
                        commands.add(commandRow);
                    }

                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(true);
                    replyKeyboardMarkup.setKeyboard(commands);

                    sendMessageRequest.setReplyMarkup(replyKeyboardMarkup);
                }
                sendMessageRequest.setText("Invalid good selected.");
            }
        } else {
            if (text.equals("Yes, I am totally sure.")) {
                Good.delete(good.getCode());
                lastCommandFinished = true;
                currentCommand = null;
                lastMessage = null;
                good = null;
                sendMessageRequest.setText("Done! The good is gone. " + Commands.HELP_COMMAND);
            } else {
                sendMessageRequest.setText("Please enter the confirmation text exactly like this:\n" +
                        "Yes, I am totally sure.\n" +
                        "\n" +
                        "Type /cancel to cancel the operation.");
            }
        }

        execute(sendMessageRequest);
    }

    private void handleIncomingMessage(Long chatId, String text, CallbackQuery callbackquery) throws TelegramApiException {
        SendMessage sendMessageRequest;
        if (!isCommand(text) && currentCommand != null && !lastCommandFinished) {
            if (currentCommand == Commands.newGoodCommand) {
                newGood(chatId, text);
            } else if (currentCommand == Commands.newLocationCommand) {
                newLocation(chatId, text);
            } else if (currentCommand == Commands.newGoodsCategoryCommand) {
                newGoodsCategory(chatId, text);
            } else if (currentCommand == Commands.listGoodsCommand) {
                listGoods(chatId, text);
            } else if (currentCommand == Commands.deleteGoodCommand) {
                deleteGood(chatId, text);
            } else if (currentCommand == Commands.searchGoodCommand) {
                searchGood(chatId, text, callbackquery);
            } else if (currentCommand == Commands.deleteGoodsCategoryCommand) {
                deleteGoodsCategory(chatId, text);
            } else if (currentCommand == Commands.deleteLocationCommand) {
                deleteLocation(chatId, text);
            } else if (currentCommand == Commands.moveGoodCommand) {
                moveGood(chatId, text);
            }
            return;
        }

        if (currentCommand != null && !text.equals(Commands.CANCEL_COMMAND) && !lastCommandFinished) {
            sendMessageRequest = new SendMessage();
            sendMessageRequest.setChatId(chatId);
            sendMessageRequest.setText("Conclua o último passo primeiro... " + lastMessage);
            execute(sendMessageRequest);
            return;
        }

        switch(handleCommand(text)) {
            case Commands.newGoodCommand:
                currentCommand = Commands.newGoodCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialNewGood(chatId);
                break;
            case Commands.searchGoodCommand:
                currentCommand = Commands.searchGoodCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialSearchGood(chatId);
                break;
            case Commands.deleteGoodCommand:
                currentCommand = Commands.deleteGoodCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialDeleteGood(chatId);
                break;
            case Commands.deleteGoodsCategoryCommand:
                currentCommand = Commands.deleteGoodsCategoryCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialDeleteGoodsCategory(chatId);
                break;
            case Commands.deleteLocationCommand:
                currentCommand = Commands.deleteLocationCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialDeleteLocation(chatId);
                break;
            case Commands.newLocationCommand:
                currentCommand = Commands.newLocationCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialNewLocation(chatId);
                break;
            case Commands.newGoodsCategoryCommand:
                currentCommand = Commands.newGoodsCategoryCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialNewGoodsCategory(chatId);
                break;
            case Commands.listGoodsCommand:
                currentCommand = Commands.listGoodsCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialListGoods(chatId);
                break;
            case Commands.moveGoodCommand:
                currentCommand = Commands.moveGoodCommand;
                lastCommandFinished = false;
                sendMessageRequest = initialMoveGood(chatId);
                break;
            case Commands.reportCommand:
                sendMessageRequest = report(chatId);
                break;
            case Commands.reportFileCommand:
                sendMessageRequest = reportFile(chatId);
                break;
            case Commands.listLocationsCommand:
                sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(chatId);

                List<Location> locations = Location.read();

                if (locations.isEmpty()) {
                    sendMessageRequest.setText("Hmm. This bot has no locations.");
                } else {
                    String message = "Locations:\n\n";
                    for (Location location : locations) {
                        message += location.toString() + "\n---\n";
                    }
                    sendMessageRequest.setText(message);
                }
                break;
            case Commands.listGoodsCategoriesCommand:
                sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(chatId);

                List<GoodsCategory> goodsCategories = GoodsCategory.read();

                if (goodsCategories.isEmpty()) {
                    sendMessageRequest.setText("Hmm. This bot has no goods categories.");
                } else {
                    String message = "Goods Categories:\n\n";
                    for (GoodsCategory goodsCategory : goodsCategories) {
                        message += goodsCategory.toString() + "\n---\n";
                    }
                    sendMessageRequest.setText(message);
                }
                break;
            case Commands.CANCEL_COMMAND:
                sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(chatId);
                if (currentCommand != null) {
                    sendMessageRequest.setText("The command " + currentCommand.substring(1) + " has been cancelled. Anything else I can do for you?\n" +
                            "\n" +
                            "Send /help for a list of commands.");
                    lastCommandFinished = true;
                    currentCommand = null;
                    lastMessage = null;
                    good = null;
                    location = null;
                    goodsCategory = null;
                    searchOperation = null;
                } else {
                    sendMessageRequest.setText("Nenhuma operação sendo realizada no momento.");
                }
                break;
            case Commands.NOTFOUND_COMMAND:
                sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(chatId);
                sendMessageRequest.setText("Unrecognized command. Say what?");
                break;
            case Commands.START_COMMAND:
            case Commands.HELP_COMMAND:
            default:
                sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(chatId);
                sendMessageRequest.enableMarkdown(true);
                sendMessageRequest.setText(HELP_TEXT);
                break;
        }

        if (handleCommand(text).equals(Commands.reportFileCommand)) {
            SendDocument sendDocument = sendFile(chatId);
            execute(sendDocument);
        }
        execute(sendMessageRequest);
    }

    private SendMessage initialNewGood(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        lastMessage = "Alright, a new good. How are we going to call it? Informe o código do bem:";
        sendMessage.setText("Alright, a new good. How are we going to call it? Informe o código do bem:");

        return sendMessage;
    }

    private SendMessage initialSearchGood(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(
            new InlineKeyboardButton().setText("Código").setCallbackData("code")
        );
        row.add(
            new InlineKeyboardButton().setText("Nome").setCallbackData("name")
        );
        row.add(
            new InlineKeyboardButton().setText("Descrição").setCallbackData("description")
        );

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setText("Choose a operation from the list below:\n\nBuscar por:");
        return sendMessage;
    }

    private SendMessage initialDeleteLocation(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> commands = new ArrayList<>();
        List<Location> locations;
        locations = Location.filterByNull();
        if (locations.isEmpty()) {
            lastCommandFinished = true;
            currentCommand = null;
            lastMessage = null;
            sendMessage.setText("Hmm. This bot has no location can delete.");
        } else {
            for (Location location : locations) {
                KeyboardRow commandRow = new KeyboardRow();
                commandRow.add("(" + location.getCode() + ") " + location.getName());
                commands.add(commandRow);
            }

            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            replyKeyboardMarkup.setKeyboard(commands);

            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            lastMessage = "Choose a location to delete.";
            sendMessage.setText("Choose a location to delete.");
        }

        return sendMessage;
    }

    private SendMessage initialDeleteGoodsCategory(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> commands = new ArrayList<>();
        List<GoodsCategory> goodsCategories;
        goodsCategories = GoodsCategory.filterByNull();
        if (goodsCategories.isEmpty()) {
            lastCommandFinished = true;
            currentCommand = null;
            lastMessage = null;
            sendMessage.setText("Hmm. This bot has no goods category can delete.");
        } else {
            for (GoodsCategory goodsCategory : goodsCategories) {
                KeyboardRow commandRow = new KeyboardRow();
                commandRow.add("(" + goodsCategory.getCode() + ") " + goodsCategory.getName());
                commands.add(commandRow);
            }

            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            replyKeyboardMarkup.setKeyboard(commands);

            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            lastMessage = "Choose a goods category to delete.";
            sendMessage.setText("Choose a goods category to delete.");
        }

        return sendMessage;
    }

    private SendMessage initialDeleteGood(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> commands = new ArrayList<>();
        List<Good> goods;
        goods = Good.read();
        if (goods.isEmpty()) {
            lastCommandFinished = true;
            currentCommand = null;
            lastMessage = null;
            sendMessage.setText("Hmm. This bot has no goods.");
        } else {
            for (Good good : goods) {
                KeyboardRow commandRow = new KeyboardRow();
                commandRow.add("(" + good.getCode() + ") " + good.getName());
                commands.add(commandRow);
            }

            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            replyKeyboardMarkup.setKeyboard(commands);

            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            lastMessage = "Choose a good to delete.";
            sendMessage.setText("Choose a good to delete.");
        }

        return sendMessage;
    }

    private SendMessage initialMoveGood(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> commands = new ArrayList<>();
        List<Good> goods;
        goods = Good.read();
        for (Good good : goods) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add("(" + good.getCode() + ") " + good.getName());
            commands.add(commandRow);
        }

        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(commands);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        lastMessage = "Please choose a good to move.";
        sendMessage.setText("Please choose a good to move.");

        return sendMessage;
    }

    private SendMessage initialListGoods(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> commands = new ArrayList<>();
        List<Location> locations;
        locations = Location.read();
        for (Location location : locations) {
            KeyboardRow commandRow = new KeyboardRow();
            commandRow.add(location.getName());
            commands.add(commandRow);
        }

        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setKeyboard(commands);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        lastMessage = "Please choose a location to get a list of its goods.";
        sendMessage.setText("Please choose a location to get a list of its goods.");

        return sendMessage;
    }

    private SendMessage initialNewLocation(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        lastMessage = "Alright, a new location. How are we going to call it? Informe o código da localização:";
        sendMessage.setText("Alright, a new location. How are we going to call it? Informe o código da localização:");

        return sendMessage;
    }

    private SendMessage initialNewGoodsCategory(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        lastMessage = "Alright, a new goods category. How are we going to call it? Informe o código da categoria do bem:";
        sendMessage.setText("Alright, a new goods category. How are we going to call it? Informe o código da categoria do bem:");

        return sendMessage;
    }

    private String textReport() {
        String message = "*Goods by location:*\n\n";
        List<Location> locations = Location.read().stream()
                .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        for (Location location : locations) {
            message += "_" + location.getName() + "_\n---\n";
            List<Good> goodsByLocation = Good.filterByLocation(location.getCode()).stream()
                    .sorted(Comparator.comparing(Good::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            for (Good goodByLocation : goodsByLocation) {
                message += goodByLocation.toString() + "\n\n";
            }
        }

        message += "*Goods by category:*\n\n";
        List<GoodsCategory> goodsCategories = GoodsCategory.read().stream()
                .sorted(Comparator.comparing(GoodsCategory::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        for (GoodsCategory goodsCategory : goodsCategories) {
            message += "_" + goodsCategory.getName() + "_\n---\n";
            List<Good> goodsByCategory = Good.filterByCategory(goodsCategory.getCode()).stream()
                    .sorted(Comparator.comparing(Good::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            for (Good goodByCategory : goodsByCategory) {
                message += goodByCategory.toString() + "\n\n";
            }
        }

        message += "*Goods by name:*\n\n";

        List<Good> goods;
        goods = Good.read();

        Set<String> namesUnique = new HashSet<>(
                goods.stream().map(Good::getName).collect(Collectors.toList())
        );
        List<String> names = new ArrayList<>(namesUnique);
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            message += "_" + name + "_\n---\n";
            List<Good> goodsByName = Good.filterByName(name).stream()
                    .sorted(Comparator.comparing(Good::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            for (Good goodByName : goodsByName) {
                message += goodByName.toString() + "\n\n";
            }
        }
        return message;
    }

    private SendDocument sendFile(Long chatId) {
        File fileToUpload = new File("report.txt");
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(fileToUpload);

        return sendDocument;
    }

    private SendMessage reportFile(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        try {
            FileWriter fileWriter = new FileWriter(
                new File("report.txt")
            );
            fileWriter.write(textReport());
            fileWriter.close();
            sendMessage.setText("Arquivo gerado com sucesso!");
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            System.out.println(e);
            sendMessage.setText("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
            System.out.println(e);
            sendMessage.setText("Error initializing stream");
        }

        return sendMessage;
    }

    private SendMessage report(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setText(textReport());
        return sendMessage;
    }

    private static String handleCommand(String text) {
        if (validCommand(text)) {
            return text;
        }

        if (isCommand(text)) {
            return Commands.NOTFOUND_COMMAND;
        }

        return text;
    }

    private static boolean validCommand(String text) {
        boolean isCommand = text.equals(Commands.START_COMMAND) || text.equals(Commands.newGoodCommand) || text.equals(Commands.moveGoodCommand) || text.equals(Commands.listGoodsCommand) || text.equals(Commands.searchGoodCommand) || text.equals(Commands.deleteGoodCommand) || text.equals(Commands.deleteLocationCommand) || text.equals(Commands.deleteGoodsCategoryCommand) || text.equals(Commands.newLocationCommand) || text.equals(Commands.listLocationsCommand) || text.equals(Commands.newGoodsCategoryCommand) || text.equals(Commands.listGoodsCategoriesCommand) || text.equals(Commands.CANCEL_COMMAND) || text.equals(Commands.reportCommand) || text.equals(Commands.reportFileCommand) || text.equals(Commands.HELP_COMMAND);
        return isCommand;
    }

    private static boolean isCommand(String text) {
        boolean isCommand = text.startsWith("/");
        return isCommand;
    }

    @Override
    public String getBotUsername() {
        return "manaiajr_bot";
    }

    @Override
    public String getBotToken() {
        return "648012033:AAGlWAxTDtbfFgCQJlA8rV6olsa7KXRZxGU";
    }
}