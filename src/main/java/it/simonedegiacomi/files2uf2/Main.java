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
        try {
            UF2BlocksUtils.packFilesToUF2(sourceFiles, destinationFile);
            System.out.println("UF2 file successfully created");
        } catch ( Exception e ) {
            System.out.println("error while creating the uf2 file:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void unpackFiles() throws IOException {
        try {
            UF2BlocksUtils.unpackUF2ToFolder(uf2SourceFile, destinationFile);
            System.out.println("UF2 file successfully extracted");
        }  catch ( Exception e ) {
            System.out.println("error while unpacking the uf2 file:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void parseArgs(String[] args) {
        if (args.length == 1 && args[0].equals("--help")) {
            printUsage();
            System.exit(0);
        } else if (args.length < 3) {
            onArgumentError("Invalid number of arguments");
        } else {
            mode = args[0];
            if ( mode.equals(PACK_MODE) ) {
                parsePackModeArgs(args);
            } else if ( mode.equals(UNPACK_MODE) ) {
                parseUnpackModeArgs(args);
            } else {
                onArgumentError("Invalid mode");
            }
        }
    }

    private static void parseUnpackModeArgs(String[] args) {
        uf2SourceFile = new File(args[1]);
        String destinationFileName = args[2];
        destinationFile = new File(destinationFileName);

        if (!destinationFile.isDirectory()) {
            onArgumentError("Invalid <destination folder> argument: " + destinationFileName + " is not a folder");
        }
    }

    private static void parsePackModeArgs(String[] args) {
        int pairsFileAndNameInContainer = args.length - 2;
        if (pairsFileAndNameInContainer % 2 != 0) {
            onArgumentError("Invalid number of arguments");
        }

        String[] pairs = Arrays.copyOfRange(args, 1, args.length - 1);

        for (int i = 0; i < pairs.length; i++) {
            String fileName = pairs[i++];
            String nameInContainer = pairs[i];

            sourceFiles.add(new UF2BlocksUtils.FileWithNameInContainer(new File(fileName), nameInContainer));
        }

        destinationFile = new File(args[args.length - 1]);
    }

    private static void onArgumentError(String error) {
        System.out.println(error);
        printUsage();
        System.exit(1);
    }

    private static void printUsage() {
        System.out.println("\nusage: files2uf2 pack <<file> <name in container>> output |");
        System.out.println("\t\t\t\t unpack <uf2 file> <destination folder>");
        System.out.println();
        System.out.println("Description: this tool can create (pack) or read (unpack) UF2 files in the 'file container' mode.");
        System.out.println();
        System.out.println("Pack example:");
        System.out.println("\tCreate a UF2 file containing two files present in the 'build' folder and name them");
        System.out.println("\tas if they were in a folder named 'Project':\n");
        System.out.println("\tfiles2uf2 pack build/file.elf Projects/file.elf build/file.rbf Projects/file.rbf build/file.uf2");
        System.out.println();
        System.out.println("Unpack example:");
        System.out.println("\tUnpack files present in the 'file.uf2' in the 'unpacked' folder:\n");
        System.out.println("\tfiles2uf2 unpack build/file.uf2 unpacked\n");
        System.out.println("\tNOTE that if the file names in the u2f file contain special characters sequences like '..' or '~'");
        System.out.println("\tthe files may be extracted outside the specified directory");
    }
}
