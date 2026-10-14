package de.yuuki.mobarmy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Archives a CLOSED Minecraft world. Native region files preserve block entities and biomes. */
final class ArenaArchive {
    static boolean allowed(Path relative){
        String name=relative.toString().replace('\\','/');
        if(name.equals("level.dat")||name.equals("level.dat_old"))return true;
        return relative.getNameCount()==2&&Set.of("region","entities","poi").contains(relative.getName(0).toString())&&relative.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca");
    }
    static void write(Path world,Path target,String arenaYaml,String report)throws IOException{
        Path root=world.toRealPath();List<Path> files;
        for(String dir:List.of("region","entities","poi"))if(Files.isSymbolicLink(root.resolve(dir)))throw new IOException("Unsafe world directory");
        try(var stream=Files.walk(root)){files=stream.filter(p->allowed(root.relativize(p))).sorted().toList();}
        long total=0;for(Path file:files){if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Unsafe world file");total+=Files.size(file);}
        if(total>256L*1024*1024||files.size()>10000)throw new IOException("Arena export exceeds 256 MiB or 10,000 files");
        if(!Files.isRegularFile(root.resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))throw new IOException("World level.dat is missing");
        Files.createDirectories(target.toAbsolutePath().getParent());Path part=target.resolveSibling(target.getFileName()+".part");
        Map<String,String> hashes=new LinkedHashMap<>();
        try{
            try(var zip=new ZipOutputStream(Files.newOutputStream(part,StandardOpenOption.CREATE_NEW))){
                add(zip,"arena.yml",arenaYaml.getBytes(StandardCharsets.UTF_8),hashes);
                add(zip,"validation.txt",report.getBytes(StandardCharsets.UTF_8),hashes);
                add(zip,"README.txt",("MobArmy arena export\nworld/ contains the saved Minecraft world. arena.yml contains its full raid definition.\nSend this complete ZIP to integrate the arena. It includes signs, containers and biomes, but no playerdata, session locks or server credentials.\nTo restore manually: STOP the server, back up its current files, extract world/ to the configured world folder and apply the week definition from arena.yml with generate-demo=false.\n").getBytes(StandardCharsets.UTF_8),hashes);
                for(Path file:files)add(zip,"world/"+root.relativize(file).toString().replace('\\','/'),Files.readAllBytes(file),hashes);
                StringBuilder checks=new StringBuilder();hashes.forEach((name,sha)->checks.append(sha).append("  ").append(name).append('\n'));zip.putNextEntry(new ZipEntry("sha256.txt"));zip.write(checks.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
            try{Files.move(part,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(part,target);}
        }finally{Files.deleteIfExists(part);}
    }
    static void add(ZipOutputStream zip,String name,byte[] bytes,Map<String,String> hashes)throws IOException{
        try{hashes.put(name,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();
    }
    static void extractWorld(Path archive,Path target)throws IOException{
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("Target world already exists");Files.createDirectories(target);Path root=target.toAbsolutePath().normalize();long total=0;
        try(var zip=new ZipInputStream(Files.newInputStream(archive))){for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){if(!entry.getName().startsWith("world/"))continue;Path relative=Path.of(entry.getName().substring(6));if(!allowed(relative))throw new IOException("Unexpected world entry");Path out=root.resolve(relative).normalize();if(!out.startsWith(root))throw new IOException("Unsafe world path");Files.createDirectories(out.getParent());
                try(var stream=Files.newOutputStream(out,StandardOpenOption.CREATE_NEW)){byte[] bytes=new byte[8192];for(int read;(read=zip.read(bytes))!=-1;){total+=read;if(total>256L*1024*1024)throw new IOException("World archive exceeds size limit");stream.write(bytes,0,read);}}
            }}
        if(!Files.isRegularFile(root.resolve("level.dat")))throw new IOException("World level.dat missing");
    }
}
