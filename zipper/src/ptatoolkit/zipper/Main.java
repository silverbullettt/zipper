package ptatoolkit.zipper;

import ptatoolkit.Global;
import ptatoolkit.Options;
import ptatoolkit.pta.basic.BasicElement;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.util.ANSIColor;
import ptatoolkit.util.Timer;
import ptatoolkit.zipper.analysis.Zipper;
import ptatoolkit.zipper.flowgraph.Dumper;
import ptatoolkit.zipper.flowgraph.ObjectFlowGraph;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Set;

public class Main {

    private static final char EOL = '\n';

    public static void main(String[] args) throws IOException {
        Options opt = Options.parse(args);
        run(opt);
    }

    public static void run(Options opt) throws IOException {
        System.out.printf("Analyze %s ...\n", opt.getApp());

        PointsToAnalysis pta = readPointsToAnalysis(opt);
        Timer zipperTimer = new Timer("Zipper Timer");
        System.out.println(ANSIColor.BOLD + ANSIColor.YELLOW + "Zipper starts ..." + ANSIColor.RESET);
        String flows = Global.getFlow() != null ? Global.getFlow() : "Direct+Wrapped+Unwrapped";
        System.out.println("Precision loss patterns: " +
                ANSIColor.BOLD + ANSIColor.GREEN + flows + ANSIColor.RESET);
        Zipper.outputNumberOfClasses(pta);
        zipperTimer.start();
        Zipper zipper = new Zipper(pta);
        Set<Method> pcm = zipper.analyze();
        zipperTimer.stop();
        System.out.print(ANSIColor.BOLD + ANSIColor.YELLOW +
                "Zipper finishes, analysis time: " + ANSIColor.RESET);
        System.out.print(ANSIColor.BOLD + ANSIColor.GREEN);
        System.out.printf("%.2fs", zipperTimer.inSecond());
        System.out.println(ANSIColor.RESET);

        if (Global.isDebug()) {
            ObjectFlowGraph ofg = Zipper.buildObjectFlowGraph(pta);
            System.out.println("Dumping object flow graph ...");
            String output = Paths.get(opt.getOutPath(), opt.getApp() + "-FG.dot")
                    .toString();
            Dumper.dumpObjectFlowGraph(ofg, output);
        }

        String expressSuffix = "-modified";
        File outDir = new File(opt.getOutPath());
        if (!outDir.exists()) {
            Files.createDirectories(outDir.toPath());
        }
        File zipperPCMOutput = new File(opt.getOutPath(),
                String.format("%s-ZipperPrecisionCriticalMethod%s%s%s.facts",
                        opt.getApp(), opt.getAnalysis(),
                        Global.isExpress() ? expressSuffix : "",
                        Global.getFlow() == null ? "" : "-" + Global.getFlow()));
        System.out.printf("Writing Zipper precision-critical methods to %s ...\n",
                zipperPCMOutput.getPath());
        System.out.println();
        writeZipperResults(pcm, zipperPCMOutput);

    }

    public static PointsToAnalysis readPointsToAnalysis(Options opt) {
        try {
            Class ptaClass = Class.forName(opt.getPTA());
            Constructor constructor = ptaClass.getConstructor(Options.class);
            return (PointsToAnalysis) constructor.newInstance(opt);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Reading points-to analysis results fails");
        }
    }

    private static void writeZipperResults(
            Set<? extends BasicElement> results, File outputFile)
            throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(outputFile);
        results.stream()
                .sorted(Comparator.comparing(BasicElement::toString))
                .forEach(method -> {
                    writer.write(method.toString());
                    writer.write(EOL);
        });
        writer.close();
    }
}
