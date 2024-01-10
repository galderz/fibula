package org.mendrugo.fibula.results;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Base64;

public final class Serializables
{
    public static String toBase64(Serializable obj)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
            oos.writeObject(obj);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        final byte[] bytes = bos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static <T extends Serializable> T fromBase64(String encoded)
    {
        final byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            return Unchecked.cast(ois.readObject());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private Serializables()
    {
    }
}
