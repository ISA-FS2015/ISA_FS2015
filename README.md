# ISA_FS2015
UMKC CS5573 ISA projects for FS2015

This tool manages the file sharing under a <b>conservative scheme</b>.

1. The files will be sent after getting an X509 certification.
2. The X509 certification will be issued after getting a direct trust mechanism.
3. The files added will be set as readonly by default for the requestor(It does not affect to original owner).
4. The files under prohibited actions against the owner will be locked(cannot read by user) until the original owner reacts.

This tool will be executed by:

```sh
./start.sh <username - SSO> <interfacename> <homedirectory> <B-backend mode>
```

<p>It should be executed as superuser or administrator.<p>
e.g.1:

```sh
sudo ./start.sh yjvxf wlan0 myfolder
```

e.g.2:

```sh
sudo ./start.sh yjvxf wlan0 myfolder B
```

using ISA_Client:

```sh
java -jar isaclient.jar 127.0.0.1 55732
```

<p> Source Code description </p>
1. 
