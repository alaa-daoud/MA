package simpleparser;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.helpers.AttributesImpl;

public class SimpleParser {

    String domainsFile = "dom.txt";
    String variablesFile = "var.txt";
    String constraintsFile = "ctr.txt";
    String outPutFile = "data.xml";
    BufferedReader in;
    StreamResult out;
    TransformerHandler th;
    AttributesImpl Attr;
    int maxBalanceAgents=10;
    //used only for creating simple problem file example 
    int maxConsts = 50;
    int maxVars = 20;
    int maxValue = 1000;
    int nbDomains = 5;
    int maxDomSize = 50;
    String simpleModel = "balance";

    public static void main(String args[]) {
        new SimpleParser().parse("simple");
    }

    HashMap<String, String> sectionElement(String element, String type) {
        HashMap currentElement = new HashMap<String, String>();
        StringTokenizer strTok = new StringTokenizer(element);
        switch (type) {
            case "domain":
                currentElement.put("name", "dom" + strTok.nextToken());
                String nbel = strTok.nextToken();
                currentElement.put("nbValues", nbel);
                currentElement.put("content", element.substring(element.indexOf(nbel) + nbel.length()));
                break;
            case "constraint":
                currentElement.put("par1", "X" + strTok.nextToken());
                currentElement.put("par2", "X" + strTok.nextToken());
                currentElement.put("src", strTok.nextToken());
                String ref = strTok.nextToken();
                if (ref.equalsIgnoreCase(">")) {
                    currentElement.put("reference", "GR");
                } else if (ref.equalsIgnoreCase("=")) {
                    currentElement.put("reference", "EQu");
                }
                currentElement.put("value", element.substring(element.indexOf(ref) + ref.length()));
                break;
            case "variable":
                currentElement.put("name", "X" + strTok.nextToken());
                currentElement.put("domain", "dom" + strTok.nextToken());
                break;
            case "agents":
                currentElement.put("name", "agent" + element);
                break;
            default:
                int col = 0;
                while (strTok.hasMoreTokens()) {
                    col++;
                    currentElement.put("par" + col, strTok.nextToken());
                }
                break;
        }
        return currentElement;
    }

    ArrayList<Map> section(BufferedReader inputReader, String type) {

        ArrayList<Map> results = new ArrayList<Map>();
        try {

            String str;
            while ((str = inputReader.readLine()) != null) {
                Map currentElement = sectionElement(str, type);
                results.add(currentElement);

            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(SimpleParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SimpleParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }

    public void parse(String method) {
        if(method.equalsIgnoreCase("simple"))
        {
            out = new StreamResult("data.xml");
            createSimpleProblem();
        }
        else
        try {
            BufferedReader domainsBufferedReader = new BufferedReader(new FileReader(domainsFile));
            BufferedReader ConstraintsBufferedReader = new BufferedReader(new FileReader(constraintsFile));
            BufferedReader variablesBufferedReader = new BufferedReader(new FileReader(variablesFile));
            out = new StreamResult("data.xml");
            openXml();
            ArrayList<Map> domains = section(domainsBufferedReader, "domain");
            ArrayList<Map> variables = section(variablesBufferedReader, "variable");
            ArrayList<Map> constraints = section(ConstraintsBufferedReader, "constraint");
            ArrayList<Map> predicates = Predicate.predicates();
            ArrayList<Map> agents = assignAgents(variables, constraints, domains, method);
            //System.err.println(variables.get(0));
            //XMLBuild(predicates.get(0), "predicate");
            buildSection(domains, "domains");
            buildSection(agents, "agents");
            System.out.println(variables.size());
            buildSection(variables, "variables");
            buildSection(constraints, "constraints");
            buildSection(predicates, "predicates");
            closeXml();
        } catch (Exception e) {
            Logger.getLogger(SimpleParser.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void createSimpleProblem() {
        try {
            ArrayList<Map> vars = new ArrayList<Map>();
            ArrayList<Map> doms = new ArrayList<Map>();
            ArrayList<Map> ctr = new ArrayList<Map>();

            Random rand = new Random();
            for (int i = 0; i < nbDomains; i++) {
                int nval = rand.nextInt(maxDomSize) + 1;
                String str = i + " " + nval;
                for (int j = 0; j < nval; j++) {
                    str += " " + (rand.nextInt(maxValue) + 1);
                }
                doms.add(sectionElement(str, "domain"));
            }
            for (int i = 0; i < maxVars; i++) {
                String str = i + " " + (rand.nextInt(nbDomains));
                vars.add(sectionElement(str, "variable"));
            }

            for (int i = 0; i < maxConsts; i++) {

                String str = (rand.nextInt(maxVars)) + " " + (rand.nextInt(maxVars)) + " ";
                int ref = rand.nextInt(5);
                switch (ref) {
                    case 0:
                        str += "D";
                        break;
                    case 1:
                        str += "C";
                        break;
                    case 2:
                        str += "F";
                        break;
                    case 3:
                        str += "P";
                        break;
                    case 4:
                        str += "L";
                        break;
                }
                int op = rand.nextInt(2);
                if (op == 1) {
                    str += " = ";
                } else {
                    str += " > ";
                }
                str += rand.nextInt(maxValue / 2);
                ctr.add(sectionElement(str, "constraint"));

            }
            ArrayList<Map> predicates = Predicate.predicates();
            ArrayList<Map> agents = assignAgents(vars, ctr, doms, simpleModel);
            openXml();
            buildSection(doms, "domains");
            buildSection(agents, "agents");
            buildSection(vars, "variables");
            buildSection(ctr, "constraints");
            buildSection(predicates, "predicates");
            closeXml();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SimpleParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(SimpleParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(SimpleParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void buildSection(ArrayList<Map> section, String lable) throws SAXException {
        Attr = new Attributes2Impl();
        String first = (lable.charAt(0) + "").toUpperCase();
        Attr.addAttribute("", "", "nb" + first + (lable.substring(1)), "", section.size() + "");
        th.startElement("", "", lable, Attr);
        for (int i = 0; i < section.size(); i++) {
            XMLBuild(section.get(i), lable.substring(0, lable.length() - 1));

        }
        th.endElement(null, null, lable);
    }

    public void XMLBuild(Map<String, String> element, String type) throws SAXException {
        switch (type) {
            case "domain":

                Attr = new Attributes2Impl();
                Attr.addAttribute("", "", "name", "", element.get("name"));
                Attr.addAttribute("", "", "nbValues", "", element.get("nbValues"));
                th.startElement("", "", "domain", Attr);
                th.characters(element.get("content").toCharArray(), 0, element.get("content").length());
                th.endElement(null, null, "domain");
                break;
            case "variable":

                Attr = new Attributes2Impl();
                Attr.addAttribute("", "", "name", "", element.get("name"));
                Attr.addAttribute("", "", "domain", "", element.get("domain"));
                Attr.addAttribute("", "", "agent", "", element.get("agent"));
                th.startElement("", "", "variable", Attr);

                th.endElement(null, null, "variable");
                break;
            case "constraint":

                Attr = new Attributes2Impl();
                Attr.addAttribute("", "", "name", "", element.get("reference") + "_" + element.get("par1") + "_" + element.get("par2"));
                Attr.addAttribute("", "", "arity", "", "3");
                Attr.addAttribute("", "", "scope", "", element.get("par1") + " " + element.get("par2"));
                Attr.addAttribute("", "", "reference", "", element.get("reference"));
                th.startElement("", "", "constraint", Attr);
                th.startElement("", "", "parameters", null);
                String con = element.get("par1") + " " + element.get("par2") + " " + element.get("value");
                System.err.println();
                System.err.println();
                System.err.println(con);
                System.err.println();
                System.err.println();
                th.characters(con.toCharArray(), 0, con.length());
                th.endElement("", "", "parameters");
                th.endElement(null, null, "constraint");
                break;
            case "agent":

                Attr = new Attributes2Impl();
                Attr.addAttribute("", "", "name", "", element.get("name"));

                th.startElement("", "", "agent", Attr);

                th.endElement(null, null, "agent");
                break;
            case "predicate":

                Attr = new Attributes2Impl();
                Attr.addAttribute("", "", "name", "", element.get("name"));

                th.startElement("", "", "predicate", Attr);

                th.startElement("", "", "parameters", null);
                th.characters(element.get("parameters").toCharArray(), 0, element.get("parameters").length());
                th.endElement(null, null, "parameters");
                th.startElement("", "", "expression", null);
                th.startElement("", "", "functional", null);
                th.characters(element.get("functional").toCharArray(), 0, element.get("functional").length());
                th.endElement(null, null, "functional");
                th.endElement(null, null, "expression");

                th.endElement(null, null, "predicate");
                break;
        }

    }

    public ArrayList<Map> assignAgents(ArrayList<Map> vars, ArrayList<Map> constraints, ArrayList<Map> doms, String method) {
        ArrayList<Map> result = new ArrayList<Map>();
        switch (method) {
            case "byDomain":
                for (int i = 0; i < doms.size(); i++) {
                    result.add(sectionElement("dom" + i, "agents"));
                }
                for (int i = 0; i < vars.size(); i++) {
                    vars.get(i).put("agent", "agent" + vars.get(i).get("domain"));
                }
                break;
            case "balance":
                if(maxBalanceAgents>vars.size())maxBalanceAgents=vars.size();
                int varPerAgent=vars.size()/maxBalanceAgents;
                for (int i = 0; i < maxBalanceAgents; i++) {
                    result.add(sectionElement("" + i, "agents"));
                }
                for (int i = 0; i < vars.size(); ) {
                    for (int j = 0; j < varPerAgent; j++) {
                       vars.get(i).put("agent", "agent" + (int)(i/varPerAgent)); 
                       i++;
                    }
                    
                }
                break;
            case "Cosite":
                Map<String,String> visited=new HashMap<>();
                int c_agent=0;
                for (int i = 0; i < constraints.size(); i++) {
                    int assig=0;
                    Map<String,String> con=constraints.get(i);
                    if(con.get("src").equalsIgnoreCase("C"))
                    {
                        String p1agent=visited.get(con.get("par1"));
                        String p2agent=visited.get(con.get("par2"));
                        if(p1agent!=null&&p2agent==null)
                        {
                            vars.get(Integer.parseInt(con.get("par2").substring(1))).put("agent", p1agent);
                            visited.put(con.get("par2"), p1agent);
                        }
                        else if(p1agent==null&&p2agent!=null)
                        {
                            vars.get(Integer.parseInt(con.get("par1").substring(1))).put("agent", p2agent); 
                            visited.put(con.get("par1"), p2agent);
                        }
                        else if(p1agent==null&&p2agent==null)
                        {
                            c_agent++;
                            result.add(sectionElement(c_agent+"", "agents"));
                            vars.get(Integer.parseInt(con.get("par1").substring(1))).put("agent", "agent"+c_agent);
                            vars.get(Integer.parseInt(con.get("par2").substring(1))).put("agent", "agent"+c_agent);
                            visited.put(con.get("par1"), "agent"+c_agent);
                            visited.put(con.get("par2"), "agent"+c_agent);
                            
                        }
                    }
                    for(int j=0;j<vars.size();i++)
                    {
                        if(visited.get(vars.get(i).get("name"))==null)
                        {
                            c_agent++;
                            result.add(sectionElement(c_agent+"", "agents"));
                            visited.put((String) vars.get(i).get("name"), "agent"+c_agent);
                            vars.get(i).put("agent", "agent"+c_agent);
                            
                        }
                    }
                        
                    
                    
                    
                }
                break;
            default:
                for (int i = 0; i < vars.size(); i++) {

                    result.add(sectionElement(i + "", "agents"));
                    vars.get(i).put("agent", "agent" + i);

                }

        }
        return result;
    }

    public void begin() {
        try {
            in = new BufferedReader(new FileReader("data.txt"));
            out = new StreamResult("data.xml");
            openXml();
            String str;
            while ((str = in.readLine()) != null) {
                process(str);
            }
            in.close();
            closeXml();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openXml() throws ParserConfigurationException, TransformerConfigurationException, SAXException {

        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        th = tf.newTransformerHandler();

        // pretty XML output
        Transformer serializer = th.getTransformer();
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");

        th.setResult(out);
        th.startDocument();
        th.startElement(null, null, "instance", null);
        // <presentation name="sampleProblem" maxConstraintArity="3"  maximize="false" format="XCSP 2.1_FRODO" />
        Attr = new Attributes2Impl();

        Attr.addAttribute("", "", "name", "", "sampleProblem");
        Attr.addAttribute("", "", "maxConstraintArity", "", "3");
        Attr.addAttribute("", "", "maximize", "", "false");
        Attr.addAttribute("", "", "format", "", "XCSP 2.1_FRODO");
        th.startElement(null, null, "presentation", Attr);
        th.endElement(null, null, "presentation");
    }

    public void process(String s) throws SAXException {
        th.startElement(null, null, "option", null);
        th.characters(s.toCharArray(), 0, s.length());
        th.endElement(null, null, "option");
    }

    public void closeXml() throws SAXException {

        th.endElement(null, null, "instance");
        th.endDocument();
    }
}

enum Predicate {
    GR, EQu;

    public static HashMap<String, String> pred(Predicate t) {
        HashMap res = new HashMap<String, String>();

        res.put("parameters", "int X int Y int val");
        if (t.equals(Predicate.GR)) {
            res.put("name", "GR");
            res.put("functional", "gt(sub(X,Y),val)");
        }
        if (t.equals(Predicate.EQu)) {
            res.put("name", "EQu");
            res.put("functional", "eq(sub(X,Y),val)");
        }
        return res;
    }

    public static ArrayList<Map> predicates() {
        ArrayList<Map> result = new ArrayList<Map>();
        result.add(pred(GR));
        result.add(pred(EQu));
        return result;
    }
}
