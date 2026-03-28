const fs = require('fs');
const path = require('path');

// Load .env file
const envPath = path.resolve(__dirname, '../.env');
const targetPath = path.resolve(__dirname, '../src/environments/environment.ts');

if (!fs.existsSync(envPath)) {
  console.warn('.env file not found, skipping environment replacement.');
  process.exit(0);
}

const envContent = fs.readFileSync(envPath, 'utf8');
const envLines = envContent.split('\n');

const envVars = {};
envLines.forEach(line => {
  const [key, value] = line.split('=');
  if (key && value) {
    envVars[key.trim()] = value.trim();
  }
});

let environmentFileContent = `export const environment = {
    production: false,
    apiUrl: 'http://localhost:8080/api',
    enableGoogleLogin: false,
    googleClientId: 'G-CLIENT-ID',
    geminiApiKey: "${envVars.GEMINI_API_KEY || 'YOUR_GEMINI_API_KEY'}",
    groqApiKey: "${envVars.GROQ_API_KEY || 'YOUR_GROQ_API_KEY'}"
};
`;

fs.writeFileSync(targetPath, environmentFileContent);
console.log(`Environment variables successfully updated in ${targetPath}`);
