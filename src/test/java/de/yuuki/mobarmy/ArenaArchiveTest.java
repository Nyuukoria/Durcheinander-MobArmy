package de.yuuki.mobarmy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
class ArenaArchiveTest {
    @TempDir Path temp;
    Path world()throws Exception{Path w=temp.resolve("world");Files.createDirectories(w.resolve("region"));Files.write(w.resolve("level.dat"),new byte[]{10,0,0,0});Files.write(w.resolve("region/r.-1.2.mca"),new byte[]{1,2,3,4});return w;}
    @Test void exportRoundTripsWorldBytesAndChecksumsButExcludesPlayerAndServerFiles()throws Exception{
        Path w=world();Files.createDirectories(w.resolve("playerdata"));Files.writeString(w.resolve("playerdata/private.dat"),"secret");Files.writeString(w.resolve("session.lock"),"lock");Files.writeString(w.resolve("server.properties"),"private");Path zip=temp.resolve("arena.zip");ArenaArchive.write(w,zip,"weeks: {}\n","VALID\n");
        try(var archive=new ZipFile(zip.toFile())){assertNull(archive.getEntry("world/playerdata/private.dat"));assertNull(archive.getEntry("world/session.lock"));assertNull(archive.getEntry("world/server.properties"));assertEquals("weeks: {}\n",new String(archive.getInputStream(archive.getEntry("arena.yml")).readAllBytes()));String checks=new String(archive.getInputStream(archive.getEntry("sha256.txt")).readAllBytes());for(String line:checks.lines().toList()){String[] parts=line.split("  ",2);byte[] bytes=archive.getInputStream(archive.getEntry(parts[1])).readAllBytes();assertEquals(parts[0],HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));}}
        Path clone=temp.resolve("clone");ArenaArchive.extractWorld(zip,clone);assertArrayEquals(Files.readAllBytes(w.resolve("region/r.-1.2.mca")),Files.readAllBytes(clone.resolve("region/r.-1.2.mca")));assertFalse(Files.exists(clone.resolve("playerdata")));assertThrows(java.io.IOException.class,()->ArenaArchive.extractWorld(zip,clone));
    }
    @Test void symlinkCannotLeakOutsideWorldAndLeavesNoArchive()throws Exception{Path w=world();Path privateFile=temp.resolve("private");Files.writeString(privateFile,"private");Files.createSymbolicLink(w.resolve("region/r.9.9.mca"),privateFile);Path zip=temp.resolve("bad.zip");assertThrows(java.io.IOException.class,()->ArenaArchive.write(w,zip,"",""));assertFalse(Files.exists(zip));assertFalse(Files.exists(temp.resolve("bad.zip.part")));}
    @Test void malformedOrTraversalArchiveCannotWriteOutsideNewWorld()throws Exception{Path zip=temp.resolve("bad.zip");try(var z=new ZipOutputStream(Files.newOutputStream(zip))){z.putNextEntry(new ZipEntry("world/../../escaped"));z.write(new byte[]{1});z.closeEntry();}assertThrows(java.io.IOException.class,()->ArenaArchive.extractWorld(zip,temp.resolve("copy")));assertFalse(Files.exists(temp.resolve("escaped")));}
    @Test void missingLevelCannotProduceValidExport()throws Exception{Path w=world();Files.delete(w.resolve("level.dat"));assertThrows(java.io.IOException.class,()->ArenaArchive.write(w,temp.resolve("missing.zip"),"",""));}
}
