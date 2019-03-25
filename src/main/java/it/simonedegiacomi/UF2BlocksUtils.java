package it.simonedegiacomi;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UF2BlocksUtils {

    private static final int UF2_BLOCK_SIZE = 512;
    private static final int UF2_BLOCK_PAYLOAD_SIZE = 256;

    private static final int UF2_START_BLOCK_MAGIC_CONSTANT_1 = 0x0A324655;
    private static final int UF2_START_BLOCK_MAGIC_CONSTANT_2 = 0x9E5D5157;
    private static final int UF2_FILE_CONTAINER_FLAG = 0x00001000;
    private static final int UF2_END_BLOCK_MAGIC_CONSTANT = 0x0AB16F30;

   public static class FileWithNameInContainer {
       private String nameInContainer;
       private File file;

       public FileWithNameInContainer(File file, String nameInContainer) {
           this.nameInContainer = nameInContainer;
           this.file = file;
       }
   }

    public static void packFilesToUF2(List<FileWithNameInContainer> files, File uf2) throws IOException {
        OutputStream out = new FileOutputStream(uf2);

        for (FileWithNameInContainer file : files) {
            List<byte[]> content = fileToByteArrays(file.file, UF2_BLOCK_PAYLOAD_SIZE);
            List<byte[]> uf2Blocks = packFilesToUF2(file.nameInContainer, content);

            for (byte[] uf2Block : uf2Blocks) {
                out.write(uf2Block);
            }
        }

        out.close();
    }

    private static List<byte[]> packFilesToUF2(String fileName, List<byte[]> fileBlocks) throws IOException {
        byte[] fileNameBytes    = fileName.getBytes(StandardCharsets.UTF_8);
        int blocksCount         = fileBlocks.size();
        List<byte[]> uf2Blocks  = new ArrayList<>(blocksCount);
        int fileSize            = (int) fileBlocks.stream().mapToLong(b -> b.length).sum();

        for (int i = 0; i < fileBlocks.size(); i++) {
            byte[] fileBlock = fileBlocks.get(i);
            byte[] uf2Block = new byte[UF2_BLOCK_SIZE];

            // Header
            setWord(uf2Block, 0, UF2_START_BLOCK_MAGIC_CONSTANT_1);
            setWord(uf2Block, 4, UF2_START_BLOCK_MAGIC_CONSTANT_2);
            setWord(uf2Block, 8, UF2_FILE_CONTAINER_FLAG); // flags
            setWord(uf2Block, 12, i * UF2_BLOCK_PAYLOAD_SIZE); // Address in flash where the data should be written
            setWord(uf2Block, 16, UF2_BLOCK_PAYLOAD_SIZE); // length
            setWord(uf2Block, 20, blocksCount);
            setWord(uf2Block, 28, fileSize);//setWord(uf2Block, 28, UF2_FAMILY_ID);

            // File content
            setBytes(uf2Block, 32, fileBlock);

            // File name
            setBytes(uf2Block, 32 + UF2_BLOCK_PAYLOAD_SIZE, fileNameBytes);

            // Footer
            setWord(uf2Block, 512 - 4, UF2_END_BLOCK_MAGIC_CONSTANT);

            uf2Blocks.add(uf2Block);
        }

        return uf2Blocks;
    }

    public static void unpackUF2ToFolder (File uf2, File destinationFolder) throws IOException {
        List<byte[]> uf2Blocks = fileToByteArrays(uf2, UF2_BLOCK_SIZE);

        String currentFileName = null;
        List<byte[]> currentPayload = new ArrayList<>();

        for (byte[] uf2Block : uf2Blocks) {
            String fileName = getString(uf2Block, 32 + UF2_BLOCK_PAYLOAD_SIZE, 512 - 4);
            byte[] payload = getBytes(uf2Block, 32, 32 + UF2_BLOCK_PAYLOAD_SIZE);

            if (fileName.equals(currentFileName)) {
                currentPayload.add(payload);
            } else {
                if (currentFileName != null) {
                    writeFile(currentPayload, destinationFolder, currentFileName);
                }

                currentPayload.clear();
                currentPayload.add(payload);
                currentFileName = fileName;
            }
        }

        writeFile(currentPayload, destinationFolder, currentFileName);
    }

    private static List<byte[]> fileToByteArrays(File file, int arraysSize) throws IOException {
        List<byte[]> blocks = new ArrayList<>();
        byte[] buffer = new byte[arraysSize];

        InputStream in = new FileInputStream(file);

        while (in.available() > 0) {
            int read = in.read(buffer, 0, arraysSize);
            blocks.add(Arrays.copyOfRange(buffer, 0, read));
        }

        in.close();

        return blocks;
    }


    private static void setBytes(byte[] block, int offset, byte[] bytes) {
        System.arraycopy(bytes, 0, block, offset, bytes.length);
    }

    private static void setWord(byte[] block, int offset, int word) {
        byte[] wordInBytes = intTo4Bytes(word);
        setBytes(block, offset, wordInBytes);
    }

    private static byte[] intTo4Bytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static String getString (byte[] bytes, int from, int maxTo) {
       int tempSize = maxTo - from;
       byte[] temp = new byte[tempSize];
       System.arraycopy(bytes, from, temp, 0, tempSize - 1);

       for (int i = 0; i < tempSize; i++) {
           if (temp[i] == 0) {
               temp = Arrays.copyOfRange(temp, 0, i);
               break;
           }
       }

       return new String(temp, StandardCharsets.UTF_8);
    }

    private static byte[] getBytes (byte[] bytes, int from, int to) {
       return Arrays.copyOfRange(bytes, from, to);
    }

    private static void writeFile(List<byte[]> content, File container, String nameInContainer) throws IOException {
       String completeName = container.getPath() + File.separator + nameInContainer;
       File file = new File(completeName);
       file.getParentFile().mkdirs();
       OutputStream out = new FileOutputStream(file);

       for (byte[] bytes : content) {
           out.write(bytes);
       }

       out.close();
    }
}
