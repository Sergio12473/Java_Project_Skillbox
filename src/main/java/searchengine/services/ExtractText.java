package searchengine.services;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@Component
@Slf4j
public class ExtractText {

    private final String regexHTMLTag = "(\\<.*?\\>)";
    private final String regexNonWordCharacter = "[^а-яА-ЯёЁ\s]";
    private final String regexNonWord = ".*(СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ|ПРЕДК)\s?.*|.*\sМС(-|\s).*";
    private final String regexSplit = "[\s\r\n]+";
    private final String regexDelEndWord = "(а|я|о|е|ь|ы|и|а|ая|ое|ой|ые|ие|ый|ий|у|ю|ем|им|ет|ит|ут|ют|ят|ал|ял|ол|ел|ул)$";
    private final String regexDelEndWordGo = "(ать|ять|оть|еть|уть|ешь|ишь|ете|ите|ала|яла|али|яли|ола|ела|оли|ели|ула|ули)$";
    private LuceneMorphology luceneMorph;
    private HashSet<String> nonCheckWord;

    public ExtractText() {
        try {
            luceneMorph = new RussianLuceneMorphology();
            nonCheckWord = new HashSet<>();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public String clearText(@NonNull String string) {
        String text = string.replaceAll(regexHTMLTag, " ");
        return text.replaceAll(regexNonWordCharacter, " ").trim();
    }

    public String[] getStrings(@NonNull String string) {
        return string.trim().split(regexSplit);
    }

    public HashMap<String, Integer> getWords(@NonNull String text) {

        String[] strings = getStrings(clearText(text));
        HashMap<String, Integer> words = new HashMap<>();

        for (String word : strings) {
            String lowWord = word.toLowerCase();
            if (!checkWord(lowWord)) {
                continue;
            }
            List<String> wordBaseForms = luceneMorph.getNormalForms(lowWord);
            String baseForms = wordBaseForms.size() > 0 ? wordBaseForms.get(0) : "";
            words.put(baseForms, words.getOrDefault(baseForms, 0) + 1);
        }
        return words;
    }

    public String delEndWord(String word) {
        String clearWord = word.replaceAll(regexDelEndWord, "");
        return word.length() < 5 ? clearWord : clearWord.replaceAll(regexDelEndWordGo, "");
    }

    private boolean checkWord(String word) {
        if (word.isEmpty()) {
            return false;
        }
        if (nonCheckWord.contains(word)) {
            return false;
        }
        List<String> wordForms = luceneMorph.getMorphInfo(word);
        for (String checkWord : wordForms) {
            if (isNonWord(checkWord)) {
                nonCheckWord.add(word);
                return false;
            }
        }
        return true;
    }

    private boolean isNonWord(String word) {
        return word.matches(regexNonWord);
    }
}