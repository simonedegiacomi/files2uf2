package it.simonedegiacomi.files2uf2;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utils to create and read UF2 files in 'file container' mode.
 * It exposes methods to pack multiple files into a single UF2 file and to unpack a UF2 file.
 */
public class UF2BlocksUtils {

    private static final int UF2_BLOCK_SIZE = 512;
    private static final int DEFAULT_UF2_BLOCK_PAYLOAD_SIZE = 256;

    private static final int UF2_START_BLOCK_MAGIC_CONSTANT_1 = 0x0A324655;
    private static final int UF2_START_BLOCK_MAGIC_CONSTANT_2 = 0x9E5D5157;
    private static final int UF2_FILE_CONTAINER_FLAG = 0x00001000;
    private static final int UF2_END_BLOCK_MAGIC_CONSTANT = 0x0AB16F30;

    private static final int OFFSET_MAGIC_CONSTANT_1 = 0;
    private static final int OFFSET_MAGIC_CONSTANT_2 = 4;
    private static final int OFFSET_FLAGS = 8;
    private static final int OFFSET_FILE_SIZE = 28;
    private static final int OFFSET_OFFSET_CURRENT_FILE = 12;
    private static final int OFFSET_BLOCK_PAYLOAD_SIZE = 16;
    private static final int OFFSET_CURRENT_FILE_BLOCKS_COUNT = 20;

    /**
     * Describe a file to pack into a UF2 file.
     */
    public static class FileWithNameInContainer {
        private String nameInContainer;
        private File file;

        /**
         * @param file            File to pack inside a UF2
         * @param nameInContainer Name that the file will have inside the UF2 file
         */
        public FileWithNameInContainer(File file, String nameInContainer) {
            this.nameInContainer = nameInContainer;
            this.file = file;
        }
    }

    public static void packFilesToUF2(List<FileWithNameInContainer> files, File uf2) throws IOException {
        OutputStream out = new FileOutputStream(uf2);

        for (FileWithNameInContainer file : files) {
            List<byte[]> content = fileToByteArrays(file.file, DEFAULT_UF2_BLOCK_PAYLOAD_SIZE);
            List<byte[]> uf2Blocks = packFilesToUF2(file.nameInContainer, content);

            for (byte[] uf2Block : uf2Blocks) {
                out.write(uf2Block);
            }
        }

        out.close();
    }

    private static List<byte[]> packFilesToUF2(String fileName, List<byte[]> fileBlocks) throws IOException {
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        int blocksCount = fileBlocks.size();
        List<byte[]> uf2Blocks = new ArrayList<>(blocksCount);
        int fileSize = (int) fileBlocks.stream().mapToLong(b -> b.length).sum();

        for (int i = 0; i < fileBlocks.size(); i++) {
            byte[] fileBlock = fileBlocks.get(i);
            byte[] uf2Block = new byte[UF2_BLOCK_SIZE];

            // Header
            setWord(uf2Block, OFFSET_MAGIC_CONSTANT_1, UF2_START_BLOCK_MAGIC_CONSTANT_1);
            setWord(uf2Block, OFFSET_MAGIC_CONSTANT_2, UF2_START_BLOCK_MAGIC_CONSTANT_2);
            setWord(uf2Block, OFFSET_FLAGS, UF2_FILE_CONTAINER_FLAG);
            setWord(uf2Block, OFFSET_OFFSET_CURRENT_FILE, i * DEFAULT_UF2_BLOCK_PAYLOAD_SIZE);
            setWord(uf2Block, OFFSET_BLOCK_PAYLOAD_SIZE, DEFAULT_UF2_BLOCK_PAYLOAD_SIZE); // length
            setWord(uf2Block, OFFSET_CURRENT_FILE_BLOCKS_COUNT, blocksCount);
            setWord(uf2Block, OFFSET_FILE_SIZE, fileSize);

            // File content
            setBytes(uf2Block, 32, fileBlock);

            // File name
            setBytes(uf2Block, 32 + DEFAULT_UF2_BLOCK_PAYLOAD_SIZE, fileNameBytes);

            // Footer
            setWord(uf2Block, 512 - 4, UF2_END_BLOCK_MAGIC_CONSTANT);

            uf2Blocks.add(uf2Block);
        }

        return uf2Blocks;
    }

    public static void unpackUF2ToFolder(File uf2, File destinationFolder) throws IOException {
        List<byte[]> uf2Blocks = fileToByteArrays(uf2, UF2_BLOCK_SIZE);

        String currentFileName = null;
        List<byte[]> currentPayload = new ArrayList<>();

        for (byte[] uf2Block : uf2Blocks) {
            assertUF2BlockInContainerMode(uf2Block);

            int payloadSize = getWord(uf2Block, OFFSET_BLOCK_PAYLOAD_SIZE);
            String fileName = getString(uf2Block, 32 + payloadSize, 512 - 4);
            byte[] payload = getBytes(uf2Block, 32, 32 + payloadSize);

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

    private static byte[] getBytes(byte[] bytes, int from, int to) {
        return Arrays.copyOfRange(bytes, from, to);
    }

    /**
     * Set 4 bytes in the bloc byte array
     *
     * @param block  block to which set the 4 bytes
     * @param offset offset to where put the bytes in the block
     * @param word   the 4 bytes to write represented as an integer
     */
    private static void setWord(byte[] block, int offset, int word) {
        byte[] wordInBytes = intToWord(word);
        setBytes(block, offset, wordInBytes);
    }

    private static int getWord (byte[] block, int offset) {
        byte[] word = getBytes(block, offset, offset + 4);
        return wordToInt(word);
    }

    private static byte[] intToWord(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static int wordToInt(byte[] word) {
        return ByteBuffer.wrap(word).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Read a string in the byte array specified starting from the the specified index up to a NULL (0) character or to
     * the maximum specified index
     *
     * @param bytes
     * @param from
     * @param maxTo
     * @return String starting from 'from' and ending with the NULL character or at length (maxTo - from)
     */
    private static String getString(byte[] bytes, int from, int maxTo) {
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

    /**
     * Write the specified list of byte arrays into a file of the specified name inside the specified container.
     * If the specified file name includes directories not present yet in the container, these will be created.
     *
     * @param content         Content of the file
     * @param container       Folder to which put the file
     * @param nameInContainer Name of the file that will be created in the container.
     * @throws IOException
     */
    private static void writeFile(List<byte[]> content, File container, String nameInContainer) throws IOException {
        String completeName = container.getPath() + File.separator + nameInContainer;
        File file = new File(completeName);

        // Create missing directories
        file.getParentFile().mkdirs();

        OutputStream out = new FileOutputStream(file);

        for (byte[] bytes : content) {
            out.write(bytes);
        }

        out.close();
    }

    private static void assertUF2BlockInContainerMode(byte[] block){
        if (block.length != UF2_BLOCK_SIZE) {
            throw new IllegalArgumentException("The specified file is not a valid UF2 file");
        }

        int flags = getWord(block, OFFSET_FLAGS);
        if (flags != UF2_FILE_CONTAINER_FLAG) {
            throw new IllegalArgumentException("The specified file is not a valid UF2 file in 'file container' mode");
        }
    }
}
