# ISA_FS2015
UMKC CS5573 ISA projects for FS2015

<p>This tool manages the file sharing under a <b>conservative scheme</b>.</p>

1. The files will be sent after getting an X509 certification.
2. The X509 certificate will be issued after getting a direct trust mechanism. The X509 certificate will be issued with common name(used as SSO), organization, location, country, private key and public key.
3. The trust will be managed by X509 certificate and scores.
4. Direct trust will be given by allowing owners to directly give X509 certificates.
5. The validation of certs will be done by not only validating the certificate itself but also checking issuer, trustee and public key with the information which owner has.
6. The scores will be increased or decreased based on how much the user contributes of file sharing or violates permissions.
7. The files added will be set as readonly by default for the requestor(It does not affect to original owner).
8. The files under prohibited actions against the owner will be locked(cannot read by user) until the original owner reacts. This will be more secure if the owner left from the network.

This tool will be executed by:

```sh
./start.sh <username - SSO> <interfacename> <homedirectory> <B-backend mode>
```

<p><b>To be more secure, It should be executed as superuser or administrator.</b></p>
<p>e.g.1:</p>

```sh
sudo ./start.sh yjvxf wlan0 myfolder
```

<p>e.g.2:</p>
<p>using backend mode and ISA_Client:</p>

```sh
sudo ./start.sh yjvxf wlan0 myfolder B &
java -jar isaclient.jar 127.0.0.1 55732
```

<p> Project directory description</p>
1. src - source code
2. jbin - executable jar files including isaclient.jar
3. res - resource files including db files and UML diagram

<p> Source Code description </p>
1. 
