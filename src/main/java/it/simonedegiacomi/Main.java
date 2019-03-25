package it.simonedegiacomi;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static final String PACK_MODE = "pack";
    private static final String UNPACK_MODE = "unpack";

    private static String mode;
    private static List<UF2BlocksUtils.FileWithNameInContainer> sourceFiles = new ArrayList<>();
    private static String uf2SourceFile;
    private static String destinationFile;


    public static void main(String[] args) throws IOException {
        parseArgs(args);

        if (mode.equals(PACK_MODE)) {
            packFiles();
        } else {
            unpackFiles();
        }
    }

    private static void packFiles() throws IOException {
        UF2BlocksUtils.fileToUF2(sourceFiles, new File(destinationFile));
    }

    private static void unpackFiles() {

    }

    private static void parseArgs(String[] args) {
        if (args.length < 4) {
            printUsageAndExit();
        }

        mode = args[1];
        if (mode.equals(PACK_MODE)) {
            int pairsFileAndNameInContainer = args.length - 3;
            if (pairsFileAndNameInContainer % 2 != 0) {
                printUsageAndExit();
            }

            String[] pairs = Arrays.copyOfRange(args, 2, args.length - 1);

            for (int i = 0; i < pairs.length; i++) {
                String fileName = pairs[i++];
                String nameInContainer = pairs[i];

                sourceFiles.add(new UF2BlocksUtils.FileWithNameInContainer(new File(fileName), nameInContainer));
            }

            destinationFile = args[args.length - 1];
        } else if (mode.equals(UNPACK_MODE)) {
            uf2SourceFile = args[2];
            destinationFile = args[3];
        } else {
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        System.out.println("usage: files2uf2 pack <<file> <name in container>> output |");
        System.out.println("\t\t\t\t unpack <uf2 file> <destination folder>");
        System.out.println();
        System.out.println("Pack example:");
        System.out.println("\tCreate a UF2 file which contains two files present in the 'build' folder and name them");
        System.out.println("\tas if they were in a folder named 'Project':\n");
        System.out.println("\tfiles2uf2 pack build/file.elf Projects/file.elf build/file.rbf Projects/file.rbf build/file.uf2");
        System.out.println();
        System.out.println("Unpack example:");
        System.out.println("\tUnpack files present in the 'file.uf2' in the 'unpacked 'folder:\n");
        System.out.println("\tfiles2uf2 unpack build/file.uf2 unpacked");

        System.exit(0);
    }


}
