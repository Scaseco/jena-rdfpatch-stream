package org.aksw.jena.rdfpatch.stream.cli.main;

import org.aksw.jena.rdfpatch.stream.cli.util.VersionProviderRDFPatchStreamCli;
import org.aksw.jena.rdfpatch.stream.cmd.CmdRDFPatchStreamApply;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name="RDFPatch Stream",
        description = "RDFPatch Stream Command Line Interface",
        versionProvider = VersionProviderRDFPatchStreamCli.class,
        subcommands = {
                CmdRDFPatchStreamApply.class
        }
)
public class CmdRDFPatchStreamMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "-v", "--version" }, versionHelp = true)
    public boolean version = false;
}
