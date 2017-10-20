# Projet checkProviderContact

Le but de ce projet est de s'assurer de la cohérence entre la base de données métier et la base de données de l'Extranet.

1) S’assurer que tous les prestataires (providerContact) d’un client (company) référencés dans la base de données métier (Informix) sont présents sur l’Extranet.
2) S’assurer que tous les prestataires référencés sur l’Extranet sont présents dans la base de données métier.

Lorsqu'une incohérence est détectée, le programme apporte une correction

##Utilisation:
```
java checkProviderContact [-dbserver db] [-webserver web] -company identifiant [-d] [-t] 
```
où :
* ```-dbserver db``` est la référence à la base de données métier, par défaut désigne la base de données de développement. Voir fichier *checkProviderContact.prop* (optionnel).
* ```-webserver web``` est la référence à la base de données de l'Extranet, par défaut désigne la base de données de développement. Voir fichier *checkProviderContact.prop* (optionnel).
* ```-company identifiant``` est l'identifiant du client à analyser (paramètre obligatoire).
* ```-d``` le programme s'exécute en mode débug, il est beaucoup plus verbeux. Désactivé par défaut (paramètre optionnel).
* ```-t``` le programme s'exécute en mode test, les transactions en base de données ne sont pas faites. Désactivé par défaut (paramètre optionnel).

##Pré-requis :
- Java 6 ou supérieur.
- JDBC Informix

##Fichier des paramètres : 

Ce fichier permet de spécifier les paramètres d'accès aux différentes bases de données.

A adapter selon les implémentations locales.

Ce fichier est nommé : *checkProviderContact.prop*.

Le fichier *checkProviderContact_Example.prop* est fourni à titre d'exemple.

##Références:

