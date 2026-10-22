# Main Project Demo — conducteur et couverture des équipes

## Lancer et présenter

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-game.ps1 -Demo
```

Dans l'IDE : `engine.Core`, argument `--demo`, dossier `res` sur le classpath.
Le présentateur joue. **Back / Restart / Next** changent l'étape uniquement à la
demande. F6/F7/F8 font la même chose ; F10 quitte. F5 suspend les événements
préparés, sans arrêter le combat ou les effets temporaires.

Les menus du haut gardent leur navigation normale. Play, Shop, Hangar, les retours,
les niveaux suivants, les résultats et le replay restent accessibles. Les textes
et commandes du bandeau sont en anglais. Le bandeau est placé sous le terrain.

## Profil persistant et répétitions

La démo utilise `.demo-profile/` à la racine du projet, ignoré par Git. Ses touches,
sa difficulté, ses volumes, ses achats, ses scores, ses records et ses succès sont
conservés entre deux lancements. Le profil normal du joueur reste distinct.

Pour une répétition neuve, utiliser un **nouveau dossier**, puis conserver ce même
chemin lors du redémarrage :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-game.ps1 -Demo -DemoProfile .\out\rehearsal-01
```

Un nouveau profil reçoit des **données de préparation annoncées** : 1000 crédits,
un classement de dix parties d'exemple (identifiants `prepared-history-*`) et sept
high scores élevés. Les succès de combat restent verrouillés pour permettre des
notifications pendant la démo. Les succès déjà acquis ne sont pas redéclenchés :
utiliser un nouveau dossier pour répéter leur premier déblocage.

## Fil conducteur

1. **Accueil et préparation — Frenchies / Audio / VFX**
   - Montrer navigation clavier puis souris, surbrillance, sons, étoiles et icônes.
   - Ouvrir Audio, régler BGM et SFX séparément, essayer M puis remettre le son.
   - Reconfigurer une touche ; montrer Échap pour annuler et le refus d'un doublon.
   - Choisir une difficulté, rouvrir Settings/Gameplay pour vérifier sa conservation.
   - Comparer les trois vaisseaux, choisir une couleur, montrer Campaign et Endless.
   - La souris immobile ne doit plus reprendre la sélection faite au clavier.
2. **Records avant la mission — Chinese can fly**
   - Montrer le classement préparé, les records personnels, les cinq conditions de
     succès, leurs états et dates de déblocage.
3. **Combat et HUD — Octopus / Friends / VFX / KimchiBaguette**
   - La première mission utilise le vaisseau, la couleur et les touches choisis.
   - La scène HUD utilise Standard : `LEVEL 1/7` entre les vies et le score.
   - Détruire un ennemi au tir normal ; rapprocher l'augmentation du score du
     `+points` localisé, puis attendre sa disparition (~1 seconde).
   - Montrer couleurs des rangées, sprites, projectiles, explosions ennemies.
   - Essayer Speedster (rafale de deux tirs, 100 ms) et Tank (trois tirs, cinq vies).
   - La scène de dégâts prépare un impact à 8 s : perte d'une vie, particules et
     clignotement rouge/couleur normale. Choisir vert au Hangar pour rouge/vert.
4. **Progression et états — Octopus / KimchiBaguette**
   - Entrée au niveau 3 avec 2/3 vies : la règle existante ajoute la troisième vie.
   - Entrée avec Tank à 5/5 : aucune sixième vie n'est ajoutée.
   - Comparer Classic / Wave / Diagonal sur trois scènes. Les adaptateurs de démo
     sélectionnent les trajectoires existantes et accélèrent leur cadence avec
     le niveau et les ennemis détruits. Les tirs visent le joueur et utilisent
     les cooldowns des tireurs de dernière rangée.
   - Comparer les tirs avec deux vies puis une vie (vitesse 4 puis 2), via
     l'adaptateur explicite de la démo.
   - Après 8 s, intercepter l'obstacle orange : vitesse de déplacement réduite de
     50 % pendant deux secondes, sans dégât et sans changement de cadence de tir.
     L'obstacle visuel est fourni par la démo ; l'effet utilise `PlayerStatus`.
5. **Objets — Best French**
   - Bouclier 15 s, Twin Cannon 15 s, Hyperfire 8 s, Chameleon Hull 5 s, grenade,
     puis laser au niveau 7. Les activations préparées commencent à 8 s.
   - Tirer pour comparer les armes ; G lance la grenade (laisser G libre).
   - Pendant Chameleon : haut/bas, contact avec ennemis, impossibilité de tirer.
   - Maintenir Espace pour le laser, même si le tir normal a été réassigné.
6. **Boss A et B — KimchiBaguette**
   - Deux rencontres autonomes : A (1000 PV, lent) puis B (800 PV, rapide).
   - Les phases sont préparées à 12/20 s ; observer les attaques simples,
     multiples, en éventail, puis l'attaque spéciale de dernière phase.
   - Détruire le dernier ennemi et le boss pour terminer la rencontre. Le
     raccordement de démo ajoute particules, un son existant de confirmation,
     comptage de défaite et record de la rencontre terminée. Il ne s'agit pas
     d'un nouveau son de boss livré par l'équipe Audio.
7. **Infini, défaite, progression et victoire**
   - Jouer en Infini, montrer zigzags et montée du rythme ; le raccourci I reste
     disponible depuis le menu principal. Next termine cette partie de démo.
   - Scène **Standard Game Over** : ne pas tirer. Un impact létal est préparé à
     8 s, avec classement déjà plein. Sans nouveau succès, montrer Game Over,
     `Stage 2 Failed`, résultats et instruction de replay dans le rendu standard.
   - Scène de progression : une formation d'un ennemi ; la détruire vivant fait
     passer réellement au niveau 2.
   - Scène finale : niveau 7 préparé avec un ennemi et un boss à 50 PV. Les
     détruire vivant pour afficher `Game Cleared!` via le vrai écran de résultats.
8. **Événements et records après la mission**
   - La scène d'événements prépare dégât à 8 s, vie à 12 s, bouclier temporaire à
     16 s et obstacle à 20 s. Lire la ligne d'événement du bandeau.
   - Consulter ensuite le panneau du bus : il affiche les événements réellement
     reçus. Les vies/boucliers passent par `GameplayEventPublisher` ; les types de
     ralentissement manquants localement utilisent un pont de démo identifié.
   - Revenir aux records et succès : retrouver les parties terminées, leurs
     scores, durées actives et notifications. Les parties interrompues ne sont
     pas ajoutées comme parties terminées. Atteindre 20 défaites / 60 secondes
     actives pour leurs succès ; aucun seuil n'est artificiellement abaissé.
   - Montrer aussi les médailles de High Scores et les icônes.
9. **Économie — HanCode**
   - La boutique utilise les vrais `ShopController`, `PurchaseSystem` et fichiers
     de sauvegarde, avec un **catalogue d'exemple explicitement annoncé**.
   - Acheter Energy Shield (100), observer crédit -100 / stock +1. Acheter Demo
     License (300), observer propriété permanente. Racheter License : refus sans
     débit. Luxury Sample : refus pour fonds insuffisants, sans changement.
   - Panneau récompenses : accumuler ennemi +10 et succès Easy/Normal/Hard
     +50/+100/+200 ; les crédits ne changent qu'après **Settle**.
   - En combat de démo, les événements de défaite accumulent aussi des récompenses
     d'exemple ; un stage réussi reçoit le bonus de difficulté. Le règlement
     transfère puis sauvegarde, sans confondre crédits et score.
   - **Buy + fail save once** : l'achat réel réussit, la sauvegarde échoue une fois.
     Noter le stock et le solde. **Retry save only** sauvegarde sans second achat,
     sans nouveau débit, sans appliquer d'effet et sans régler de nouvelles récompenses.
   - Scène d'utilisation : acheter du stock avant, laisser B libre puis appuyer B.
     Le vrai service consomme une unité et active le bouclier, sans nouveau débit.
     Maintenir B ou essayer pendant un bouclier actif ne consomme pas d'autre unité.
10. **Persistance et récupération**
    - **Reload from disk** recharge crédits, quantités, propriétés et records.
    - **Test recovery on a copy** corrompt uniquement une copie isolée et exerce
      la récupération réelle depuis son backup. Le profil actif reste intact.
    - Quitter puis relancer avec le même profil ; vérifier commandes, difficulté,
      achats et succès. Rouvrir la boutique et les records via les menus normaux.

## Limites d'intégration à annoncer

Le dépôt local reste la référence : plusieurs comportements annoncés par les
équipes ne sont pas encore raccordés dans son lancement normal. Le catalogue
commercial de production est vide ; Boss B n'y est pas sélectionné ; le mouvement
reste fixé sur Diagonal ; il n'y a pas d'obstacle visuel intégré. Les scènes
préparées identifient donc leurs adaptateurs au lieu de les attribuer à un merge
qui n'est pas présent localement. La navigation libre ne sélectionne pas ces
adaptateurs de combat à l'insu du joueur.

**JSON** : modèles immuables, grille et interfaces `LevelReader`/`LevelWriter`
existent ; aucun lecteur/écrivain JSON ni point d'entrée n'est présent. Le panneau
final l'explique. La démo ne prétend pas réaliser un import/export opérationnel.

Les compteurs `icône ×N` du fork Friends, le HUD de monnaie, les popups Endless et
le recentrage non mergé des résultats avec succès sont exclus. Les bonus d'armes
restent présentés dans leurs propres scènes Best French, pas comme une livraison
Friends. Tous les écrans du jeu normal restent disponibles via la navigation.

## Vérifier

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-demo.ps1
```

Les tests couvrent boutons, navigation libre, transactions acceptées/refusées,
échec et reprise de sauvegarde, quantités consommées, récompenses, variantes de
combat et persistance dans un **second processus Java**. Les paramètres de durée
`res/demo.properties`, `-DemoScale`, `-DemoScript` ne servent qu'au contrôle
accéléré opt-in (`-Dinvaders.demo.autoAdvance=true`), pas au passage des scènes en
présentation. Une répétition avec le présentateur reste nécessaire pour le son,
le projecteur et le déroulé oral.