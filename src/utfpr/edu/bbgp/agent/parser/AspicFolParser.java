package utfpr.edu.bbgp.agent.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
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
import utfpr.edu.bbgp.extended.ResourceFolFormula;

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
        // ASpicParser Code
        final Pattern ORDER = Pattern.compile(".*<.*(\\s*#.*)?");
        final Pattern PLANS = Pattern.compile("(.+):-([^\\$]+)(\\$(.+))?");
        final Pattern PLAN_BEL_CONTEXT = Pattern.compile("(((!\\s*)*)?([A-Za-z0-9]+(\\w*)*((\\([A-Za-z0-9]+(\\w)*(,\\s*[A-Za-z0-9]+(\\w)*)*\\)))?))");
        final Pattern PLAN_RES_CONTEXT = Pattern.compile("((!\\s*)?res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?))");
        final Pattern PLAN_BEL_POSTCOND = Pattern.compile("((\\+|\\-)\\s*([A-Za-z0-9]+(\\w*)*((\\([A-Za-z0-9]+(\\w)*(,\\s*[A-Za-z0-9]+(\\w)*)*\\)))?))");
        final Pattern PLAN_RES_POSTCOND = Pattern.compile("(\\+|\\-)\\s*(res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?))");
        final Pattern RESOURCES = Pattern.compile("\\s*res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?)\\s*(\\s*#.*)?");

        AspicArgumentationTheoryFol as = new AspicArgumentationTheoryFol(rfGen);

        BufferedReader br = new BufferedReader(reader);
        String line;
        Matcher m, m2, m_res;
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            
            m = PLANS.matcher(line);
            m2 = RESOURCES.matcher(line);
            if (ORDER.matcher(line).matches()) {
                as.setOrder(parseSimpleOrder(line.split("#")[0]));
            } else if (m.matches()) {
                FolFormula goalFormula = Agent.parseFolFormulaSafe(m.group(1), folParser.getSignature());

                String context = m.group(2);
                String newContext = context;
                String postConditions = m.group(4);
                postConditions = postConditions.split("#")[0].trim();

                m_res = PLAN_RES_CONTEXT.matcher(context);

                HashSet<FolFormula> beliefContext = new HashSet<>();
                HashSet<ResourceFolFormula> resourceContext = new HashSet<>();

                while (m_res.find()) {
                    String element = context.substring(m_res.start(), m_res.end());
                    newContext = newContext.replaceFirst(element, "");

                    String[] split = element.split(":");
                    if (split.length == 2) {
                        split = split[1].split(",");

                        String resource = split[0].trim();
                        Double value = Double.parseDouble(split[1].trim());

                        if (element.startsWith("!")) {
                            value *= -1;
                        }

                        resourceContext.add(new ResourceFolFormula(resource, value));
                    }
                }
                
                m = PLAN_BEL_CONTEXT.matcher(newContext);

                while (m.find()) {
                    String element = newContext.substring(m.start(), m.end());

                    FolFormula belief = Agent.parseFolFormulaSafe(element, folParser.getSignature());
                    beliefContext.add(belief);
                }

                ArrayList<PerceptionEntry> postConditionsSet = new ArrayList<>();

                if (postConditions != null) {
                    String newPostConditions = postConditions;
                    m_res = PLAN_RES_POSTCOND.matcher(postConditions);
                    while(m_res.find()){
                        String element = postConditions.substring(m_res.start(), m_res.end());
                        newPostConditions = newPostConditions.replaceFirst(element.replaceAll("\\+", "\\\\+").replaceAll("\\-", "\\\\-"), "");
                        
                        char operation = element.charAt(0);
                        element = element.substring(1).trim();

                        String[] split = element.split(":");
                        if (split.length == 2) {
                            split = split[1].split(",");

                            String resource = split[0].trim();
                            Double value = Double.parseDouble(split[1].trim());

                            postConditionsSet.add(new PerceptionEntry(resource, value, operation));
                        }
                    }
                    
                    m = PLAN_BEL_POSTCOND.matcher(newPostConditions);

                    while (m.find()) {
                        String element = newPostConditions.substring(m.start(), m.end());

                        char operation = element.charAt(0);
                        element = element.substring(1).trim();
                        
                        FolFormula belief = Agent.parseFolFormulaSafe(element, folParser.getSignature());
                        postConditionsSet.add(new PerceptionEntry(belief, operation));
                    }
                }

                as.addPlanTemplate(goalFormula, beliefContext, resourceContext, postConditionsSet);
            } else if (m2.matches()) {
                String element = line.split("#")[0].trim();

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
                    as.addRule((InferenceRule<FolFormula>) rule);
                }
            }
        }

        return as;
    }

    @Override
    public Formula parseFormula(Reader reader) throws IOException, ParserException {
        final Pattern RULE = Pattern.compile("(.*)(" + symbolStrict + "|" + symbolDefeasible + ")(.+)"),
                RULE_ID = Pattern.compile("^\\s*([A-Za-z0-9]+)\\s*:(.*)"),
                RULE_RES_BODY = Pattern.compile("((!\\s*)?res\\s*:\\s*([A-Za-z0-9]+\\s*,\\s*[0-9]+(.[0-9]+)?))"),
                RULE_BEL_BODY = Pattern.compile("\\s*(!)*\\s*([A-Za-z0-9]+(\\w*)*(\\(([A-Za-z0-9]|\\s|,)+\\))?)\\s*"),
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
            rule.setConclusion(Agent.parseFolFormulaSafe(m.group(3).split("#")[0], folParser.getSignature()));
            String str = m.group(1);
            m = RULE_ID.matcher(str);
            if (m.matches()) {
                rule.setName(m.group(1));
                str = m.group(2);
            }
            if (!EMPTY.matcher(str).matches()) {
                m = RULE_RES_BODY.matcher(str);
                String str2 = str;
                
                while (m.find()) {
                    String element = str.substring(m.start(), m.end());
                    str2 = str.replaceAll(element, "");
                    
                    String[] split = element.split(":");
                    if (split.length == 2) {
                        split = split[1].split(",");

                        String resource = split[0].trim();
                        Double value = Double.parseDouble(split[1].trim());
                        
                        if (element.startsWith("!")) {
                            value *= -1;
                        }
                        
                        rule.addPremise(new ResourceFolFormula(resource, value));
                    }
                }
                
                m = RULE_BEL_BODY.matcher(str2);

                while (m.find()) {
                    String s = str2.substring(m.start(), m.end());

                    rule.addPremise(Agent.parseFolFormulaSafe(s, folParser.getSignature()));
                }
            }
            return rule;
        }
        return null;
    }
}
