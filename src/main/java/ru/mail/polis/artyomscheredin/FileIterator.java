package ru.mail.polis.artyomscheredin;

import ru.mail.polis.BaseEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Iterator;

import static ru.mail.polis.artyomscheredin.Utils.readEntry;

public class FileIterator implements Iterator<BaseEntry<ByteBuffer>> {
    ByteBuffer dataBuffer;
    ByteBuffer indexBuffer;
    int upperBound;
    int pointer = 0;

    public FileIterator(Utils.Pair<Path> paths, ByteBuffer from, ByteBuffer to) throws IOException {
        try (FileChannel dataChannel = FileChannel.open(paths.dataPath());
             FileChannel indexChannel = FileChannel.open(paths.indexPath())) {
            indexBuffer = indexChannel.map(FileChannel.MapMode.READ_ONLY, 0, indexChannel.size());
            dataBuffer = dataChannel.map(FileChannel.MapMode.READ_ONLY, 0, dataChannel.size());
        }

        int lowerBound = (from == null) ? 0 : findOffset(indexBuffer, dataBuffer, from);
        upperBound = (to == null) ? indexBuffer.limit() : findOffset(indexBuffer, dataBuffer, to);
        pointer = indexBuffer.getInt(lowerBound);
    }

    private static int findOffset(ByteBuffer indexBuffer, ByteBuffer dataBuffer, ByteBuffer key) {
        int low = 0;
        int mid = 0;
        int high = indexBuffer.remaining() / Integer.BYTES - 1;
        while (low <= high) {
            mid = low + ((high - low) / 2);
            int offset = indexBuffer.getInt(mid * Integer.BYTES);
            int keySize = dataBuffer.getInt(offset);

            ByteBuffer curKey = dataBuffer.slice(offset + Integer.BYTES,  keySize);
            if (curKey.compareTo(key) < 0) {
                low = mid + 1;
            } else if (curKey.compareTo(key) > 0) {
                high = mid - 1;
            } else if (curKey.compareTo(key) == 0) {
                return mid*Integer.BYTES;
            }
        }
        indexBuffer.rewind();
        dataBuffer.rewind();
        return mid*Integer.BYTES;
    }

    @Override
    public boolean hasNext() {
        return pointer > upperBound;
    }

    @Override
    public BaseEntry<ByteBuffer> next() {
        BaseEntry<ByteBuffer> temp = readEntry(dataBuffer, pointer);
        pointer = indexBuffer.getInt();
        return temp;
    }
}
