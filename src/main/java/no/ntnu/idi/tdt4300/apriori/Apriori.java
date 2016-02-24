package no.ntnu.idi.tdt4300.apriori;

import org.apache.commons.cli.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the main class of the association rule generator.
 * <p>
 * It's a dummy reference program demonstrating the accepted command line arguments, input file format and standard output
 * format also required from your implementation. The generated standard output follows the CSV (comma-separated values) format.
 * <p>
 * It's up to you if you use this program as your base, however, it's very important to strictly follow the given formatting
 * of the inputs and outputs. Your assignment will be partly automatically evaluated, therefore keep the input arguments
 * and output format identical.
 * <p>
 * Alright, I believe it's enough to stress three times the importance of the input and output formatting. Four times...
 *
 * @author tdt4300-undass@idi.ntnu.no
 */
public class Apriori {

    /**
     * Loads the transaction from the ARFF file.
     *
     * @param filepath relative path to ARFF file
     * @return list of transactions as sets
     * @throws java.io.IOException signals that I/O error has occurred
     */
    public static List<SortedSet<String>> readTransactionsFromFile(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        List<String> attributeNames = new ArrayList<String>();
        List<SortedSet<String>> itemSets = new ArrayList<SortedSet<String>>();

        String line = reader.readLine();
        while (line != null) {
            if (line.contains("#") || line.length() < 2) {
                line = reader.readLine();
                continue;
            }
            if (line.contains("attribute")) {
                int startIndex = line.indexOf("'");
                if (startIndex > 0) {
                    int endIndex = line.indexOf("'", startIndex + 1);
                    attributeNames.add(line.substring(startIndex + 1, endIndex));
                }
            } else {
                SortedSet<String> is = new TreeSet<String>();
                StringTokenizer tokenizer = new StringTokenizer(line, ",");
                int attributeCounter = 0;
                String itemSet = "";
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    if (token.equalsIgnoreCase("t")) {
                        String attribute = attributeNames.get(attributeCounter);
                        itemSet += attribute + ",";
                        is.add(attribute);
                    }
                    attributeCounter++;
                }
                itemSets.add(is);
            }
            line = reader.readLine();
        }
        reader.close();

        return itemSets;
    }

    /**
     * Generates the frequent itemsets given the support threshold. The results are returned in CSV format.
     *
     * @param transactions list of transactions
     * @param support      support threshold
     * @return frequent itemsets in CSV format with columns size and items; columns are semicolon-separated and items are comma-separated
     */
    public static ArrayList<ConcurrentHashMap<ArrayList<String>, Double>> generateFrequentItemsets(List<SortedSet<String>> transactions, double support) {
        // TODO: Generate and print frequent itemsets given the method parameters.
        int transactionSize = transactions.size();
        //Items to be generated
        ArrayList<ConcurrentHashMap<ArrayList<String>, Double>> items = new ArrayList<ConcurrentHashMap<ArrayList<String>, Double>>();
        //Candidates
        ArrayList<ArrayList<String>> candidates = new ArrayList<ArrayList<String>>();
        //Initial adding of candidates
        for (SortedSet<String> list: transactions) {
            for (String s : list) {
                ArrayList<String> temp = new ArrayList<String>();
                temp.add(s);
                if (!candidates.contains(temp)) {
                    candidates.add(temp);
                }
            }
        }
        //Loop until no more candidates can be generated
        while (candidates.size() > 0) {
            //The items that are to be added this iteration
            ConcurrentHashMap<ArrayList<String>, Double> currentLevel = new ConcurrentHashMap<ArrayList<String>, Double>();
            //Looping over transactions, checking which contains the candidates.
            for (int x = 0; x < transactions.size(); x++) {
                for (ArrayList<String> candidate : candidates) {
                    if (transactions.get(x).containsAll(candidate)) {
                        //Puting a new entry into the hashmap, or +1 if it already exists
                        if (!checkContainment(currentLevel.keySet(), candidate)) {
                            currentLevel.put(candidate, 1.0);
                        } else {
                            currentLevel.put(candidate, currentLevel.get(candidate) + 1.0);
                        }
                    }
                }
            }
            //Checking if the candidates match the required support, removing those that doesent.
            for (Map.Entry<ArrayList<String>, Double> entry : currentLevel.entrySet()) {
                if ((entry.getValue() / transactionSize < support)) {
                    currentLevel.remove(entry.getKey());
                }
            }
            items.add(currentLevel);
            //Generate candidates for the next iteration
            candidates = generateNewCandidates(new ArrayList<ArrayList<String>>(items.get(items.size() - 1).keySet()));
        }
        return items;
    }

    private static ArrayList<ArrayList<String>> generateNewCandidates(ArrayList<ArrayList<String>> items) {
        ArrayList<ArrayList<String>> returnList = new ArrayList<ArrayList<String>>();
        if (items.size() == 0) {
            return returnList;
        }
        int creationLength = items.get(0).size();

        for (int x = 0; x < items.size(); x++) {
            for (int y = x + 1; y < items.size(); y++) {
                ArrayList<String> common = new ArrayList<String>(items.get(x));
                common.retainAll(items.get(y));
                if (common.size() == creationLength - 1) {
                    Set<String> hs = new HashSet<String>();
                    hs.addAll(items.get(x));
                    hs.addAll(items.get(y));
                    ArrayList<String> toAdd = new ArrayList<String>(hs);
                    if (!returnList.contains(toAdd)) {
                        returnList.add(new ArrayList<String>(hs));
                    }
                }
            }
        }
        return returnList;
    }
    private static boolean checkContainment(ConcurrentHashMap.KeySetView<ArrayList<String>, Double> a, ArrayList<String> b) {

        if (a.size() == 0) {
            return false;
        }
        for (ArrayList<String> arr : a) {
            if (arr.equals(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates the association rules given the support and confidence threshold. The results are returned in CSV
     * format.
     *
     * @param transactions list of transactions
     * @param support      support threshold
     * @param confidence   confidence threshold
     * @return association rules in CSV format with columns antecedent, consequent, confidence and support; columns are semicolon-separated and items are comma-separated
     */
    public static String generateAssociationRules(List<SortedSet<String>> transactions, double support, double confidence) {
        // TODO: Generate and print association rules given the method parameters.

        //Generate the frequent itemsets.
        ArrayList<ConcurrentHashMap<ArrayList<String>, Double>> items = generateFrequentItemsets(transactions, support);
        int transactionSize = transactions.size();
        String returnString = "";
        //Create a flattened Hashmap
        ConcurrentHashMap<ArrayList<String>, Double> flatItems = flatten(items);
        //Loop over each entry
        for (Map.Entry<ArrayList<String>, Double> entry : flatItems.entrySet()) {
            String[] set = entry.getKey().toArray(new String[entry.getKey().size()]);
            //generate all of the subsets for the given entry.
            ArrayList<ArrayList<String>> allSubsets = getAllSubsets(set);
            //remove the empty subset, as well as the full subset.
            allSubsets.remove(0);
            allSubsets.remove(allSubsets.size() - 1);
            //For each of the subsets, perform s -> (l - s) and append to the string that is to be returned.
            if (allSubsets.size() > 0) {
                for (ArrayList<String> current : allSubsets) {
                    Double currentConf = (entry.getValue() / flatItems.get(current));
                    if (currentConf >= confidence) {

                        ArrayList<String> print = new ArrayList<String>(entry.getKey());
                        for (String s : current) {
                            if (print.contains(s)) {
                                print.remove(s);
                            }
                        }

                        returnString += current.toString().replace("[", "").replace("]", "").replace(" ", "") + ";"
                                + print.toString().replace("[", "").replace("]", "").replace(" ", "") + ";"
                                + Math.round(currentConf * 100) / 100.0 + ";"
                                + Math.round((entry.getValue() / transactionSize) * 100) /100.0
                                + "\n";
                        }
                    }
                }
            }

        return returnString;
        /*
        return "antecedent;consequent;confidence;support\n" +
                "diapers;beer;0.6;0.5\n" +
                "beer;diapers;1.0;0.5\n" +
                "diapers;bread;0.8;0.67\n" +
                "bread;diapers;0.8;0.67\n" +
                "milk;bread;0.8;0.67\n" +
                "bread;milk;0.8;0.67\n" +
                "milk;diapers;0.8;0.67\n" +
                "diapers;milk;0.8;0.67\n" +
                "diapers,milk;bread;0.75;0.5\n" +
                "bread,milk;diapers;0.75;0.5\n" +
                "bread,diapers;milk;0.75;0.5\n" +
                "bread;diapers,milk;0.6;0.5\n" +
                "milk;bread,diapers;0.6;0.5\n" +
                "diapers;bread,milk;0.6;0.5\n";
        */
    }
    private static ConcurrentHashMap<ArrayList<String>, Double> flatten(ArrayList<ConcurrentHashMap<ArrayList<String>, Double>> items) {
        ConcurrentHashMap<ArrayList<String>, Double> flatItems = new ConcurrentHashMap<ArrayList<String>, Double>();
        for (ConcurrentHashMap<ArrayList<String>, Double> map : items) {
            for (Map.Entry<ArrayList<String>, Double> entry : map.entrySet()) {
                Collections.sort(entry.getKey());
                flatItems.put(entry.getKey(), entry.getValue());
            }
        }
        return flatItems;
    }

    private static ArrayList<ArrayList<String>> getAllSubsets(String[] currentItems) {
        ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
        if(currentItems.length == 0){
            res.add(new ArrayList<String>());
            return res;
        }
        Arrays.sort(currentItems);
        String head = currentItems[0];
        String[] rest = new String[currentItems.length-1];
        System.arraycopy(currentItems, 1, rest, 0, currentItems.length-1);
        for(ArrayList<String> list : getAllSubsets(rest)){
            ArrayList<String> newList = new ArrayList<String>();
            newList.add(head);
            newList.addAll(list);
            res.add(list);
            res.add(newList);
        }
        return res;
    }


    private static String output(ArrayList<ConcurrentHashMap<ArrayList<String>, Double>> items) {
        String returnString = "";
        for (ConcurrentHashMap<ArrayList<String>, Double> x : items) {
            for (Map.Entry<ArrayList<String>, Double> entry : x.entrySet()) {
                returnString += Integer.toString(entry.getKey().size()) + ";";
                for (String item : entry.getKey()) {
                    returnString += item + ",";
                }
                returnString = returnString.substring(0, returnString.length() - 1);
                returnString += "\n";
            }
        }
        return returnString;
    }

    /**
     * Main method.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // definition of the accepted command line arguments
        Options options = new Options();
        options.addOption(Option.builder("f").argName("file").desc("input file with transactions").hasArg().required(true).build());
        options.addOption(Option.builder("s").argName("support").desc("support threshold").hasArg().required(true).build());
        options.addOption(Option.builder("c").argName("confidence").desc("confidence threshold").hasArg().required(false).build());
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            // extracting filepath and support threshold from the command line arguments
            String filepath = cmd.getOptionValue("f");
            double support = Double.parseDouble(cmd.getOptionValue("s"));

            // reading transaction from the file
            List<SortedSet<String>> transactions = readTransactionsFromFile(filepath);

            if (cmd.hasOption("c")) {
                // extracting confidence threshold
                double confidence = Double.parseDouble(cmd.getOptionValue("c"));

                // printing generated association rules
                System.out.println(generateAssociationRules(transactions, support, confidence));
            } else {
                // printing generated frequent itemsets
                System.out.println(output(generateFrequentItemsets(transactions, support)));
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setOptionComparator(null);
            helpFormatter.printHelp("java -jar apriori.jar", options, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
