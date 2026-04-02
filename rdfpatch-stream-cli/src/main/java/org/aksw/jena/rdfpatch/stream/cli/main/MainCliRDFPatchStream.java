package org.aksw.jena.rdfpatch.stream.cli.main;

import picocli.CommandLine;

public class MainCliRDFPatchStream {
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new CmdRDFPatchStreamMain());
        commandLine.execute(args);
    }
}
