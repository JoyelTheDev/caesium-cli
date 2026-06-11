package dev.sim0n.caesium;

import dev.sim0n.caesium.config.CaesiumConfig;
import dev.sim0n.caesium.config.ConfigLoader;
import dev.sim0n.caesium.exception.CaesiumException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(
    name = "caesium",
    mixinStandardHelpOptions = true,
    description = "Java Bytecode Obfuscator"
)
public class Start implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the YAML config file", paramLabel = "CONFIG")
    private File configFile;

    @Option(names = {"-i", "--input"}, description = "Override input JAR path from config")
    private File inputOverride;

    @Option(names = {"-o", "--output"}, description = "Override output JAR path from config")
    private File outputOverride;

    @Override
    public Integer call() throws Exception {
        CaesiumConfig config = ConfigLoader.load(configFile);

        File input  = inputOverride  != null ? inputOverride  : new File(config.getInput());
        File output = outputOverride != null ? outputOverride : resolveOutput(config, input);

        Caesium caesium = new Caesium();
        caesium.getMutatorManager().applyConfig(config);

        if (config.getDictionary() != null) {
            try {
                caesium.setDictionary(dev.sim0n.caesium.util.Dictionary.valueOf(config.getDictionary().toUpperCase()));
            } catch (IllegalArgumentException e) {
                Caesium.getLogger().warn("Unknown dictionary '{}', using default NUMBERS", config.getDictionary());
            }
        }

        PreRuntime.loadJavaRuntime();

        if (config.getLibraries() != null) {
            for (String lib : config.getLibraries()) {
                PreRuntime.libraries.addElement(lib);
            }
        }

        PreRuntime.loadClassPath();
        PreRuntime.loadInput(input.getAbsolutePath());
        PreRuntime.buildInheritance();

        return caesium.run(input, output);
    }

    private File resolveOutput(CaesiumConfig config, File input) {
        if (config.getOutput() != null && !config.getOutput().isBlank())
            return new File(config.getOutput());
        return new File(input.getName().replace(".jar", "-obf.jar"));
    }

    public static void main(String[] args) {
        int code = new CommandLine(new Start()).execute(args);
        System.exit(code);
    }
}
