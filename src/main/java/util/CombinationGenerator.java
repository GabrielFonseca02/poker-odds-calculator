package util;
import com.GabrielFonseca.pokeroddscalculator.model.*;

import java.util.ArrayList;
import java.util.List;

public class CombinationGenerator {

    public static List<List<Card>> generate(List<Card> cards) {

        List<List<Card>> combinations = new ArrayList<>();

        generateCombinations(cards, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    private static void generateCombinations(List<Card> cards,
                                             int start,
                                             List<Card> current,
                                             List<List<Card>> combinations){


        if (current.size() == 5) {
            combinations.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < cards.size(); i++) {

            current.add(cards.get(i));

            generateCombinations(
                    cards,
                    i + 1,
                    current,
                    combinations
            );

            current.remove(current.size() - 1);
        }
    }
}

