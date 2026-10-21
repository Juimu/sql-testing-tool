package dev.sqlnext;

import java.nio.channels.*;
import java.nio.file.*;
import java.io.IOException;

/** One application instance per workspace prevents conflicting task recovery. */
public final class WorkspaceLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    public WorkspaceLock(Path directory)throws IOException {
        Files.createDirectories(directory);channel=FileChannel.open(directory.resolve("workspace.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        FileLock acquired;
        try {acquired=channel.tryLock();if(acquired==null)throw new IOException("Another SQLNext instance is using this workspace.");}
        catch(IOException|OverlappingFileLockException e){channel.close();throw new IOException("Another SQLNext instance is using this workspace, or the workspace cannot be locked.",e);}
        lock=acquired;
    }
    @Override public void close()throws IOException {try{lock.release();}finally{channel.close();}}
}
