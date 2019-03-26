package it.simonedegiacomi.files2uf2;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static final String PACK_MODE = "pack";
    private static final String UNPACK_MODE = "unpack";

    private static String mode;
    private static List<UF2BlocksUtils.FileWithNameInContainer> sourceFiles = new ArrayList<>();
    private static File uf2SourceFile;
    private static File destinationFile;


    public static void main(String[] args) throws IOException {
        parseArgs(args);

        if (mode.equals(PACK_MODE)) {
            packFiles();
        } else {
            unpackFiles();
        }
    }

    private static void packFiles() throws IOException {
        UF2BlocksUtils.packFilesToUF2(sourceFiles, destinationFile);
        System.out.println("UF2 file successfully created");
    }

    private static void unpackFiles() throws IOException {
        UF2BlocksUtils.unpackUF2ToFolder(uf2SourceFile, destinationFile);
        System.out.println("UF2 file successfully extracted");
    }

    private static void parseArgs(String[] args) {
        if (args.length < 3) {
            printUsageAndExit();
        }

        mode = args[0];
        if (mode.equals(PACK_MODE)) {
            int pairsFileAndNameInContainer = args.length - 2;
            if (pairsFileAndNameInContainer % 2 != 0) {
                printUsageAndExit();
            }

            String[] pairs = Arrays.copyOfRange(args, 1, args.length - 1);

            for (int i = 0; i < pairs.length; i++) {
                String fileName = pairs[i++];
                String nameInContainer = pairs[i];

                sourceFiles.add(new UF2BlocksUtils.FileWithNameInContainer(new File(fileName), nameInContainer));
            }

            destinationFile = new File(args[args.length - 1]);
        } else if (mode.equals(UNPACK_MODE)) {
            uf2SourceFile = new File(args[1]);
            destinationFile = new File(args[2]);

            if (!destinationFile.isDirectory()) {
                printUsageAndExit();
            }
        } else {
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        System.out.println("usage: files2uf2 pack <<file> <name in container>> output |");
        System.out.println("\t\t unpack <uf2 file> <destination folder>");
        System.out.println();
        System.out.println("Description: this tool can create (pack) or read (unpack) UF2 files in the 'file container' mode.");
        System.out.println();
        System.out.println("Pack example:");
        System.out.println("\tCreate a UF2 file which contains two files present in the 'build' folder and name them");
        System.out.println("\tas if they were in a folder named 'Project':\n");
        System.out.println("\tfiles2uf2 pack build/file.elf Projects/file.elf build/file.rbf Projects/file.rbf build/file.uf2");
        System.out.println();
        System.out.println("Unpack example:");
        System.out.println("\tUnpack files present in the 'file.uf2' in the 'unpacked' folder:\n");
        System.out.println("\tfiles2uf2 unpack build/file.uf2 unpacked");

        System.exit(0);
    }


}
