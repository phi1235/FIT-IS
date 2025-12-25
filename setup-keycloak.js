const http = require('http');

const post = (path, data, token) => {
    return new Promise((resolve, reject) => {
        const headers = { 'Content-Type': 'application/json' };
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const req = http.request({
            hostname: 'localhost',
            port: 8080,
            path: path,
            method: 'POST',
            headers: headers
        }, res => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) resolve(body ? JSON.parse(body) : {});
                else reject(`Error ${res.statusCode}: ${body}`);
            });
        });
        req.on('error', reject);
        req.write(JSON.stringify(data));
        req.end();
    });
};

const getToken = () => {
    return new Promise((resolve, reject) => {
        const data = "client_id=admin-cli&username=admin&password=admin&grant_type=password";
        const req = http.request({
            hostname: 'localhost',
            port: 8080,
            path: '/realms/master/protocol/openid-connect/token',
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }, res => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                if (res.statusCode === 200) resolve(JSON.parse(body).access_token);
                else reject(`Auth failed: ${body}`);
            });
        });
        req.write(data);
        req.end();
    });
};

const run = async () => {
    try {
        console.log("Getting token...");
        const token = await getToken();
        console.log("Got token.");

        console.log("Creating realm 'phi-realm'...");
        try {
            await post('/admin/realms', { realm: 'phi-realm', enabled: true }, token);
            console.log("Realm created.");
        } catch (e) {
            if (e.toString().includes("409")) console.log("Realm already exists.");
            else throw e;
        }

        console.log("Creating client 'spring-boot-app'...");
        try {
            await post('/admin/realms/phi-realm/clients', {
                clientId: 'spring-boot-app',
                bearerOnly: true,
                protocol: 'openid-connect'
            }, token);
            console.log("Client spring-boot-app created.");
        } catch (e) {
            if (e.toString().includes("409")) console.log("Client spring-boot-app exists.");
            else console.log("Client spring-boot-app error: " + e);
        }

        console.log("Creating client 'angular-app'...");
        try {
            await post('/admin/realms/phi-realm/clients', {
                clientId: 'angular-app',
                publicClient: true,
                redirectUris: ['http://localhost:4200/*'],
                webOrigins: ['*'],
                protocol: 'openid-connect'
            }, token);
            console.log("Client angular-app created.");
        } catch (e) {
            if (e.toString().includes("409")) console.log("Client angular-app exists.");
            else console.log("Client angular-app error: " + e);
        }

    } catch (e) {
        console.error("Setup failed:", e);
        process.exit(1);
    }
};

run();
