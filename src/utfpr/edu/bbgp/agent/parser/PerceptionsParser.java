/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.tweety.logics.fol.parser.FolParser;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import net.sf.tweety.logics.fol.syntax.FolSignature;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.PerceptionEntry;

/**
 *
 * @author henri
 */
public class PerceptionsParser {

    public static ArrayList<PerceptionEntry> parserPerceptionFile(FolSignature agentSignature, Reader iReader) {
        if (agentSignature == null) {
            throw new NullPointerException("Agent signature is null");
        }
        if (iReader == null) {
            throw new NullPointerException("Input stream is null");
        }

        ArrayList<PerceptionEntry> list = new ArrayList<>();

        FolParser parser = new FolParser();
        parser.setSignature(agentSignature);

        BufferedReader reader = new BufferedReader(iReader);
        String line;

        Pattern ENTRY = Pattern.compile("^\\s*([0-9]+)\\s*:\\s*(\\+|\\-)\\s*((!\\s*)?(\\w+(\\(\\s*\\w+(\\s*,\\s*\\w+)*\\s*\\))?))\\s*$");
        final Pattern PLAN_POSTCOND = Pattern.compile("^\\s*([0-9]+)\\s*:\\s*(((\\+|\\-)\\s*([A-Za-z0-9]+(\\([A-Za-z0-9]+(,\\s*[A-Za-z0-9]+)*\\))))|((\\+|\\-)\\s*(res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?))))\\s*$");
//        final Pattern RESOURCES = Pattern.compile("\\s*res\\s*:\\s*([A-Za-z0-9]+,[0-9]+(.[0-9]+)?)\\s*");

        try {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = ENTRY.matcher(line);
//                if (matcher.matches()) {
//                    String[] splited = line.split(":");
//                    if (splited.length != 2) {
//                        continue;
//                    }
//                    Integer cycle = Integer.parseInt(matcher.group(1));
//                    Character operation = matcher.group(2).charAt(0);
//                    FolFormula perception = Agent.parseFolFormulaSafe(matcher.group(3), agentSignature);
//
//                    list.add(new PerceptionEntry(cycle.longValue(), list.size() + 1, operation, perception));
//                }
                    matcher = PLAN_POSTCOND.matcher(line);

                    while (matcher.find()) {
//                        String element = line.substring(matcher.start(), matcher.end());

                        Integer cycle = Integer.parseInt(matcher.group(1));
                        String entry = matcher.group(2);
                        
                        char operation = entry.charAt(0);
                        entry = entry.substring(1).trim();

                        String[] split = entry.split(":");
                        if (split.length == 2) {
                            split = split[1].split(",");

                            String resource = split[0].trim();
                            Double value = Double.parseDouble(split[1].trim());

                            list.add(new PerceptionEntry(cycle.longValue(), list.size() + 1, operation, resource, value));
                        } else {
//                            FolFormula belief = (FolFormula) folParser.parseFormula(element);
                            FolFormula belief = Agent.parseFolFormulaSafe(entry, agentSignature);
                            list.add(new PerceptionEntry(cycle.longValue(), list.size() + 1, operation, belief));
                        }
                    }
            }
        } catch (IOException ex) {
        }

        return list;
    }
}
