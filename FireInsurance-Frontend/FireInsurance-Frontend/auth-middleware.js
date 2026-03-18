const fs = require('fs');
const path = require('path');

// Simple JWT mock (not real JWT - just for testing)
function generateMockToken(user) {
  const payload = {
    userId: user.id,
    email: user.email,
    role: user.role,
    exp: Date.now() + 24 * 60 * 60 * 1000 // 24 hours
  };
  return 'mock_jwt_' + Buffer.from(JSON.stringify(payload)).toString('base64');
}

module.exports = (req, res, next) => {
  // Enable CORS
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  
  // Handle preflight
  if (req.method === 'OPTIONS') {
    res.sendStatus(200);
    return;
  }

  // Log all requests for debugging
  console.log(`${req.method} ${req.url} - Path: ${req.path}`);
  
  // Handle login endpoint - match both /auth/login and /api/auth/login
  if (req.method === 'POST' && (req.path === '/api/auth/login' || req.path === '/auth/login')) {
    const { email, password } = req.body;
    
    console.log('Login attempt:', { email, password, body: req.body });

    // Read users from db.json
    const dbPath = path.join(__dirname, 'db.json');
    const db = JSON.parse(fs.readFileSync(dbPath, 'utf-8'));
    
    console.log('Available users:', db.users.map(u => ({ email: u.email, role: u.role })));
    
    // Find user
    const user = db.users.find(u => u.email === email && u.password === password);

    if (user) {
      const token = generateMockToken(user);
      res.status(200).json({
        token: token,
        userId: user.id,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        role: user.role
      });
    } else {
      res.status(401).json({
        message: 'Invalid credentials'
      });
    }
    return;
  }

  // Handle register customer endpoint - match both paths
  if (req.method === 'POST' && (req.path === '/api/auth/register/customer' || req.path === '/auth/register/customer')) {
    const customerData = req.body;
    
    // Read and update db.json
    const dbPath = path.join(__dirname, 'db.json');
    const db = JSON.parse(fs.readFileSync(dbPath, 'utf-8'));
    
    // Check if email already exists
    if (db.users.find(u => u.email === customerData.email)) {
      res.status(400).json({ message: 'Email already exists' });
      return;
    }

    const newUser = {
      id: db.users.length + 1,
      ...customerData,
      role: 'CUSTOMER',
      createdAt: new Date().toISOString()
    };

    db.users.push(newUser);
    fs.writeFileSync(dbPath, JSON.stringify(db, null, 2));

    res.status(201).json({
      message: 'Customer registered successfully',
      userId: newUser.id
    });
    return;
  }

  // Handle register surveyor endpoint - match both paths
  if (req.method === 'POST' && (req.path === '/api/auth/register/surveyor' || req.path === '/auth/register/surveyor')) {
    const surveyorData = req.body;
    
    const dbPath = path.join(__dirname, 'db.json');
    const db = JSON.parse(fs.readFileSync(dbPath, 'utf-8'));
    
    if (db.users.find(u => u.email === surveyorData.email)) {
      res.status(400).json({ message: 'Email already exists' });
      return;
    }

    const newUser = {
      id: db.users.length + 1,
      ...surveyorData,
      role: 'SURVEYOR',
      createdAt: new Date().toISOString()
    };

    db.users.push(newUser);
    fs.writeFileSync(dbPath, JSON.stringify(db, null, 2));

    res.status(201).json({
      message: 'Surveyor registered successfully',
      userId: newUser.id
    });
    return;
  }

  // Verify token for protected routes
  if (req.path.startsWith('/api/') && !req.path.startsWith('/api/auth/')) {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ message: 'Missing or invalid token' });
      return;
    }

    // Skip actual token validation for mock server
    // In real scenario, you would decode and verify the JWT
  }

  // Continue to JSON Server router
  next();
};
