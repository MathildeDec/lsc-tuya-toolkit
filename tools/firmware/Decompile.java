// Décompile des fonctions en pseudo-C. Arguments : fichier de sortie, puis noms ou adresses (0x...) de fonctions.
// @category Firmware
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import java.io.PrintWriter;

public class Decompile extends GhidraScript {
    @Override
    public void run() throws Exception {
        String[] a = getScriptArgs();
        DecompInterface di = new DecompInterface();
        di.openProgram(currentProgram);
        try (PrintWriter w = new PrintWriter(a[0])) {
            for (int i = 1; i < a.length; i++) {
                Function f = null;
                if (a[i].startsWith("0x")) {
                    Address ad = toAddr(Long.parseLong(a[i].substring(2), 16));
                    f = getFunctionAt(ad);
                } else {
                    for (Function g : currentProgram.getFunctionManager().getFunctions(true)) {
                        if (g.getName().equals(a[i])) { f = g; break; }
                    }
                }
                w.println("// ===== " + a[i]);
                if (f == null) { w.println("// introuvable"); continue; }
                DecompileResults r = di.decompileFunction(f, 60, monitor);
                w.println(r.decompileCompleted() ? r.getDecompiledFunction().getC() : "// échec : " + r.getErrorMessage());
            }
        }
    }
}
