package br.com.armandoalmeida;

import br.com.armandoalmeida.pointer.MouseManager;

public class MouseIdleStats {

    public static void main(String[] args) {
        boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");
        new MouseManager(keepOsAlive).run();
    }

}
