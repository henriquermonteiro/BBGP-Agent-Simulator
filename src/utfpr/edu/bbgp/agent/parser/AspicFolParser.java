/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utfpr.edu.bbgp.agent.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.tweety.arg.aspic.parser.AspicParser;
import net.sf.tweety.arg.aspic.ruleformulagenerator.RuleFormulaGenerator;
import net.sf.tweety.arg.aspic.syntax.AspicArgumentationTheory;
import net.sf.tweety.arg.aspic.syntax.DefeasibleInferenceRule;
import net.sf.tweety.arg.aspic.syntax.InferenceRule;
import net.sf.tweety.arg.aspic.syntax.StrictInferenceRule;
import net.sf.tweety.commons.BeliefBase;
import net.sf.tweety.commons.Formula;
import net.sf.tweety.commons.Parser;
import net.sf.tweety.commons.ParserException;
import net.sf.tweety.logics.fol.parser.FolParser;
import net.sf.tweety.logics.fol.syntax.FolFormula;
import utfpr.edu.bbgp.agent.Agent;
import utfpr.edu.bbgp.agent.PerceptionEntry;
import utfpr.edu.bbgp.extended.AspicArgumentationTheoryFol;

/**
 *
 * @author henri
 */
public class AspicFolParser extends AspicParser<FolFormula> {

    private final FolParser folParser;
    private final RuleFormulaGenerator<FolFormula> rfGen;
    
    private final String symbolStrict = "->";
    private final String symbolDefeasible = "=>";
    private final String symbolComma = ",";

    public AspicFolParser(Parser<? extends BeliefBase, ? extends Formula> formulaparser, RuleFormulaGenerator<FolFormula> rfg) {
        super(formulaparser, rfg);

        folParser = (FolParser) formulaparser;
        rfGen = rfg;
    }

    public FolParser getFolParser() {
        return folParser;
    }

    @Override
    public AspicArgumentationTheory<FolFormula> parseBeliefBase(Reader reader) throws IOException, ParserException {
        // FolParser code

        String s = "";
        // for keeping track of the section of the file:
        // 0 means sorts declaration
        // 1 means type declaration, i.e. functor/predicate declaration
        // 2 means formula section
//        int section = 0;
        // Read formulas and separate them with "\n" (ascii code 10)
//        try {
//            for (int c = reader.read(); c != -1; c = reader.read()) {
//                if (c == 10) {
//                    s = s.trim();
//                    if (!s.equals("")) {
//                        if (s.startsWith("type")) {
//                            section = 1;
//                        } else if (section == 1) {
//                            section = 2; //A type declaration section has been parsed previously, 
//                        }
//                        //therefore only the formula section remains.
//                        if (section == 2) {
//                            break;
//                        } else if (section == 1) {
//                            folParser.parseTypeDeclaration(s, folParser.getSignature());
//                        } else {
//                            folParser.parseSortDeclaration(s, folParser.getSignature());
//                        }
//                    }
//                    s = "";
//                } else {
//                    s += (char) c;
//                }
//            }
//        } catch (IOException | ParserException e) {
//            throw new ParserException(e);
//        }

        // ASpicParser Code
        final Pattern ORDER = Pattern.compile(".*<.*");
        final Pattern PLANS = Pattern.compile("(.+):-([^\\$]+)(\\$(.+))?");
        final Pattern PLAN_CONTEXT = Pattern.compile("(((!\\s*)*)?([A-Za-z0-9]+(\\([A-Za-z0-9]+(,\\s*[A-Za-z0-9]+)*\\))))|(res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?))");
        final Pattern PLAN_POSTCOND = Pattern.compile("((\\+|\\-)\\s*([A-Za-z0-9]+(\\([A-Za-z0-9]+(,\\s*[A-Za-z0-9]+)*\\))))|(\\+|\\-)\\s*(res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?))");
        final Pattern RESOURCES = Pattern.compile("\\s*res\\s*:\\s*([A-Za-z0-9]+,[0-9]+(.[0-9]+)?)\\s*");

//        AspicArgumentationTheory<FolFormula> ass = new AspicArgumentationTheory<>(rfGen);
        AspicArgumentationTheoryFol as = new AspicArgumentationTheoryFol(rfGen);

        BufferedReader br = new BufferedReader(reader);
        String line = s; // Feed the first line
        Matcher m, m2;
        while (true) {
            m = PLANS.matcher(line);
            m2 = RESOURCES.matcher(line);
            if (ORDER.matcher(line).matches()) {
//                ass.setOrder(parseSimpleOrder(line));
                as.setOrder(parseSimpleOrder(line));
            } else if (m.matches()) {
//                FolFormula goalFormula = (FolFormula) folParser.parseFormula(m.group(1));
                FolFormula goalFormula = Agent.parseFolFormulaSafe(m.group(1), folParser.getSignature());

                String context = m.group(2);
                String postConditions = m.group(4);

                m = PLAN_CONTEXT.matcher(context);

                HashSet<FolFormula> beliefContext = new HashSet<>();
                HashMap<String, Double> resourceContext = new HashMap<>();

                while (m.find()) {
                    String element = context.substring(m.start(), m.end());

                    String[] split = element.split(":");
                    if (split.length == 2) {
                        split = split[1].split(",");

                        String resource = split[0].trim();
                        Double value = Double.parseDouble(split[1].trim());

                        resourceContext.put(resource, value);
                    } else {
//                        FolFormula belief = (FolFormula) folParser.parseFormula(element);
                        FolFormula belief = Agent.parseFolFormulaSafe(element, folParser.getSignature());
                        beliefContext.add(belief);
                    }
                }

                HashSet<PerceptionEntry> postConditionsSet = new HashSet<>();

                if (postConditions != null) {
                    m = PLAN_POSTCOND.matcher(postConditions);

                    while (m.find()) {
                        String element = postConditions.substring(m.start(), m.end());

                        char operation = element.charAt(0);
                        element = element.substring(1).trim();

                        String[] split = element.split(":");
                        if (split.length == 2) {
                            split = split[1].split(",");

                            String resource = split[0].trim();
                            Double value = Double.parseDouble(split[1].trim());

                            postConditionsSet.add(new PerceptionEntry(resource, value, operation));
                        } else {
//                            FolFormula belief = (FolFormula) folParser.parseFormula(element);
                            FolFormula belief = Agent.parseFolFormulaSafe(element, folParser.getSignature());
                            postConditionsSet.add(new PerceptionEntry(belief, operation));
                        }
                    }
                }

                as.addPlanTemplate(goalFormula, beliefContext, resourceContext, postConditionsSet);
            } else if (m2.matches()) {
                String element = line.trim();

                String[] split = element.split(":");
                if (split.length == 2) {
                    split = split[1].split(",");

                    String resource = split[0].trim();
                    Double value = Double.parseDouble(split[1].trim());

                    as.addResource(resource, value);
                }
            } else {
                Formula rule = parseFormula(line);
                if (rule != null) {
//                    ass.addRule((InferenceRule<FolFormula>) rule);
                    as.addRule((InferenceRule<FolFormula>) rule);
                }
            }
            line = br.readLine(); // Moved to the end of the loop
            if (line == null) {
                break;
            }
        }

        return as;
    }

    @Override
    public Formula parseFormula(Reader reader) throws IOException, ParserException {
        final Pattern RULE = Pattern.compile("(.*)(" + symbolStrict + "|" + symbolDefeasible + ")(.+)"),
                RULE_ID = Pattern.compile("^\\s*([A-Za-z0-9]+)\\s*:(.*)"),
                RULE_BODY = Pattern.compile("\\s*(!)*\\s*([A-Za-z0-9]+\\(([A-Za-z0-9]|\\s|,)+)\\)\\s*"), // new pattern
                EMPTY = Pattern.compile("^\\s*$");

        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        if (line == null) {
            return null;
        }
        Matcher m = RULE.matcher(line);
        if (m.matches()) {
            InferenceRule<FolFormula> rule
                    = m.group(2).equals(symbolDefeasible)
                    ? new DefeasibleInferenceRule<>()
                    : new StrictInferenceRule<>();
            rule.setConclusion(Agent.parseFolFormulaSafe(m.group(3), folParser.getSignature()));
            String str = m.group(1);
            m = RULE_ID.matcher(str);
            if (m.matches()) {
                rule.setName(m.group(1));
                str = m.group(2);
            }
            if (!EMPTY.matcher(str).matches()) {
                m = RULE_BODY.matcher(str); // Changed to use pattern and avoid the comma error on predicates with two or more terms

                while (m.find()) {
                    String s = str.substring(m.start(), m.end());

                    rule.addPremise(Agent.parseFolFormulaSafe(s, folParser.getSignature()));
                } // End of changes
            }
            return rule;
        }
        return null;
    }
}
