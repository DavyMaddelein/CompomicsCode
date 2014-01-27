package com.compomics.compomicscode.oneofs.mascodatfilecsvparser;

import com.compomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import com.compomics.mascotdatfile.util.interfaces.Modification;
import com.compomics.mascotdatfile.util.mascot.PeptideHit;
import com.compomics.mascotdatfile.util.mascot.ProteinHit;
import com.compomics.mascotdatfile.util.mascot.Query;
import com.compomics.mascotdatfile.util.mascot.enumeration.MascotDatfileType;
import com.compomics.mascotdatfile.util.mascot.factory.MascotDatfileFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {

    public static void main(String[] args) {
        try {
            File inputFile = null;
            File outputFile = null;
            MascotDatfileInf mascotDatFile;

            Options options = new Options();
            options.addOption("i", true, "mascot datfile to parse");
            options.addOption("o", true, "output location");
            options.addOption("h", false, "print this message");

            CommandLineParser parser = new BasicParser();

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("i")) {
                try {
                    inputFile = new File(cmd.getOptionValue("i"));
                    if (cmd.hasOption("o")) {
                        outputFile = new File(cmd.getOptionValue("o"), String.format("%s_parsed.csv", inputFile.getName()));
                    } else {
                        outputFile = new File(inputFile.getParentFile(), inputFile.getName() + "parsed.csv");
                    }
                } catch (NullPointerException npe) {
                    System.out.println("please provide an inputfile");
                    //npe.printStackTrace();
                    System.exit(1);
                }
                System.out.println("started with parsing");
                mascotDatFile = MascotDatfileFactory.create(inputFile.getAbsolutePath(), MascotDatfileType.INDEX);
                System.out.println("done parsing, starting with output");
                Enumeration<Query> queryEnum = mascotDatFile.getQueryEnumerator();
                FileWriter outputWriter = new FileWriter(outputFile);
                outputWriter.append("spectrum title;theoretic mass;charge;experimental mass;rank;passThreshold;Mascot expectancy;"
                        + "mascot score;peptide in multiple proteins;peptide unique to one protein;sequence;modified;modifications;start;end;accession");
                while (queryEnum.hasMoreElements()) {
                    int rank = 1;
                    Query aQuery = queryEnum.nextElement();
                    for (PeptideHit aHit : mascotDatFile.getQueryToPeptideMap().getAllPeptideHits(aQuery.getQueryNumber())) {
                        StringBuilder outputString = new StringBuilder();
                        outputString.append(aQuery.getTitle()).append(";");
                        outputString.append(aHit.getPeptideMr()).append(";"); //theoretic mass
                        outputString.append(aQuery.getChargeString()).append(";");
                        outputString.append(String.valueOf(aHit.getPeptideMr() + aHit.getDeltaMass())).append(";");
                        outputString.append(rank).append(";");
                        outputString.append(aHit.scoresAboveIdentityThreshold()).append(";");
                        outputString.append(aHit.getExpectancy()).append(";");
                        outputString.append(aHit.getIonsScore()).append(";");
                        ArrayList<ProteinHit> proteinHits = aHit.getProteinHits();
                        if (proteinHits.isEmpty()) {
                            outputString.append("not matched with a protein");
                        } else {
                            if (proteinHits.size() == 1) {
                                outputString.append("false;true;");
                            } else {
                                outputString.append("true;false;");
                            }
                            outputString.append(aHit.getSequence()).append(";");
                            outputString.append(aHit.getModifiedSequence()).append(";");
                            boolean modificationFound = false;
                            if (aHit.getModifications() != null) {
                                for (Modification aModification : aHit.getModifications()) {
                                    if (aModification != null) {
                                        modificationFound = true;
                                        outputString.append(aModification.getShortType()).append(",");
                                    }
                                }
                                if (modificationFound) {
                                    outputString.deleteCharAt(outputString.lastIndexOf(","));
                                }
                            }
                            outputString.append(";");
                            ProteinHit proteinHit = (ProteinHit) aHit.getProteinHits().get(0);
                            outputString.append(proteinHit.getStart()).append(";");
                            outputString.append(proteinHit.getStop()).append(";");
                            outputString.append(proteinHit.getAccession());
                        }
                        outputWriter.append("\n");
                        outputWriter.append(outputString);
                        outputWriter.flush();
                        rank++;
                    }
                }
                outputWriter.close();
                System.out.println("finished");
            } else if (cmd.hasOption("h")) {
                System.out.println("this parser outputs a parsed datfile in a semi-colon(;) separated file");
                for (Option anOption : (Collection<Option>) options.getOptions()) {
                    StringBuilder outputString = new StringBuilder();
                    outputString.append("-").append(anOption.getOpt()).append("\t").append(anOption.getDescription());
                    if (anOption.hasValueSeparator()) {
                        outputString.append(" ").append("arguments seperated by").append(anOption.getValueSeparator());
                    }
                    System.out.println(outputString.toString());
                }
                System.exit(0);
            } else {
                System.out.println("please provide an input file, you can get the options by specifying -h");
                System.exit(1);
            }
        } catch (ParseException ex) {
            System.out.println("could not parse the arguments");
            //ex.printStackTrace();
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("there was a problem reading the mascot datfile");
            //ex.printStackTrace();
            System.exit(1);
        }
    }
}
