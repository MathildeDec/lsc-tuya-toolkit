# Comment décompiler sans les sources

Le firmware (`anyka_ipc`) est un binaire ARM 32 bits **non strippé** : il garde les noms de fonctions publiques (`IPC_APP_*`, `ht_*`) et plein de chaînes de log (`xxx.c`, noms de clés de config). Ghidra reconstruit du pseudo-C à partir du code machine ; ce n'est pas la source d'origine, mais c'est suffisant pour lire la logique.

## 1. Ce qu'il faut

- Le binaire `anyka_ipc` extrait de **sa propre caméra** (il n'est pas dans le dépôt : propriétaire).
- Ghidra 12.2 + JDK 25 (voir [README.md](README.md), « Reproduire »).
- Projet déjà analysé : `analyzeHeadless <projet> anyka -import anyka_ipc -processor ARM:LE:32:v5t` (≈ 15 min, une seule fois).

## 2. Obtenir le pseudo-C d'une fonction

    analyzeHeadless <projet> anyka -process anyka_ipc -noanalysis \
      -scriptPath tools/firmware -postScript Decompile.java sortie.c \
      0x7a664 IPC_APP_set_flip_onoff

Arguments : fichier de sortie, puis des **adresses** (`0x...`) ou des **noms**. Les adresses des gestionnaires de DP sont dans [DP_TABLE.md](DP_TABLE.md), les noms dans `tools/gateway/dp_table.h`. Pour le détail interactif : ouvrir le projet dans Ghidra (fenêtre *Decompile*, `L` pour renommer, `;` pour commenter).

## 3. Méthode (ordre à suivre)

1. **Gestionnaire du DP** (adresse dans DP_TABLE.md) : il convertit la charge utile cloud, appelle `IPC_APP_set_*`, relit avec `get_*` puis renvoie l'état. Le premier appel (`FUN_...(param_1)`) est la conversion ; le second argument de la fin (ex. `0x67`) est le numéro de DP renvoyé.
2. **`IPC_APP_set_*`** : repérer
   - la **clé de config** écrite (`ht_tuya_config_set_boolean("…", v)`) : elle donne le nom persistant ;
   - la fonction qui applique l'effet (`ht_tuya_func_set_*`, `ht_*`) ;
   - les **bornes** (`if (v < 0 || v > 2)`) → valeurs admises.
3. **Suivre l'appel** jusqu'à la fonction `ht_*`/`ak_*` qui agit sur le matériel, avec le graphe : `awk -F'\t' '$1==nom' calls.tsv`, ou [FLOWS.md](FLOWS.md).
4. **Retrouver les chaînes** : Ghidra affiche `DAT_xxxx + 0xyyyy` pour une chaîne adressée relativement (ARM). L'adresse réelle est la somme ; la chaîne est dans `strings.tsv` (fonction → chaîne) ou dans Ghidra (*Go To* l'adresse).
5. **Types** : Ghidra met `undefined4` partout. Déduire : comparaison à 0/1 → booléen ; bornes → énumération ; passage à `sprintf("%d")` → entier ; pointeur + longueur → tampon.
6. **Confirmer** quand c'est possible sur caméra (via Mathilde) et le noter dans l'issue.

## 4. Pièges

- `wirte_log(...)` (faute dans le firmware) = journal : ses chaînes décrivent le but de la fonction.
- Les `FUN_xxxxxxxx` n'ont pas de symbole : le rôle se déduit de ce qu'ils appellent.
- Les appels par pointeur de fonction n'apparaissent pas dans le graphe (sauf la table des DP).
- Le pseudo-C peut être faux (types, boucles) : recouper avec `objdump -d` si une valeur compte.

## 5. Livrable attendu pour un lot

Dans `docs/firmware/DP_VALUES.md` (section du lot), par DP : adresse du gestionnaire, `IPC_APP_*`, clé de config, valeurs admises, effet, persistance (oui/non), niveau de confiance (lu / confirmé sur caméra). Ne **pas** coller de grands blocs de pseudo-C dans le dépôt : citer adresse + fonction + une ligne utile.
