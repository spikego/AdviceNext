class DebugDashboard {
    constructor() {
        this.selectedModule = null;
        this.updateInterval = null;
        this.init();
    }

    init() {
        this.setupTabs();
        this.setupEventListeners();
        this.startUpdates();
        this.loadModules();
        this.loadPlayers();
        this.setupMinimap();
    }

    setupTabs() {
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tabId = btn.dataset.tab;
                
                tabBtns.forEach(b => b.classList.remove('active'));
                tabContents.forEach(c => c.classList.remove('active'));
                
                btn.classList.add('active');
                document.getElementById(tabId).classList.add('active');
            });
        });
    }

    setupEventListeners() {
        // Chat input
        document.getElementById('chatInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });

        // Console input
        document.getElementById('consoleInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.executeCommand();
            }
        });

        // Movement controls
        document.querySelectorAll('.control-btn').forEach(btn => {
            btn.addEventListener('mousedown', () => {
                this.startMovement(btn.dataset.action);
            });
            btn.addEventListener('mouseup', () => {
                this.stopMovement(btn.dataset.action);
            });
        });
    }

    startUpdates() {
        this.updateInterval = setInterval(() => {
            this.updateSystemInfo();
            this.updateConsole();
            this.updateChat();
            this.updatePlayers();
        }, 1000);
    }

    async updateSystemInfo() {
        try {
            const response = await fetch('/api/system');
            const data = await response.json();
            
            document.getElementById('fps').textContent = `FPS: ${data.fps}`;
            document.getElementById('memory').textContent = `Memory: ${data.memory} MB`;
            document.getElementById('position').textContent = `Pos: ${data.x}, ${data.y}, ${data.z}`;
        } catch (error) {
            console.error('Failed to update system info:', error);
        }
    }

    async loadModules() {
        try {
            const response = await fetch('/api/modules');
            const data = await response.json();
            
            const modulesList = document.getElementById('modulesList');
            modulesList.innerHTML = '';
            
            data.modules.forEach(module => {
                const moduleItem = document.createElement('div');
                moduleItem.className = 'module-item';
                moduleItem.innerHTML = `
                    <span class="module-name">${module.name}</span>
                    <div class="module-toggle ${module.enabled ? 'enabled' : ''}"></div>
                `;
                
                const toggle = moduleItem.querySelector('.module-toggle');
                toggle.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.toggleModule(module.name, !module.enabled);
                });
                
                if (module.hasSettings) {
                    moduleItem.addEventListener('click', (e) => {
                        if (!e.target.classList.contains('module-toggle')) {
                            this.selectModule(module.name, moduleItem);
                        }
                    });
                    moduleItem.style.cursor = 'pointer';
                }
                
                modulesList.appendChild(moduleItem);
            });
        } catch (error) {
            console.error('Failed to load modules:', error);
        }
    }

    async selectModule(moduleName, element) {
        // Update UI
        document.querySelectorAll('.module-item').forEach(item => {
            item.classList.remove('selected');
        });
        element.classList.add('selected');
        
        this.selectedModule = moduleName;
        
        try {
            const response = await fetch(`/api/settings?module=${moduleName}`);
            const data = await response.json();
            
            const settingsDiv = document.getElementById('moduleSettings');
            settingsDiv.innerHTML = `<h4>${moduleName} Settings</h4>`;
            
            data.settings.forEach(setting => {
                const settingDiv = document.createElement('div');
                settingDiv.className = 'setting-item';
                
                let inputHtml = '';
                if (setting.type === 'Boolean') {
                    inputHtml = `<input type="checkbox" class="setting-input" ${setting.value === 'true' ? 'checked' : ''} 
                                onchange="dashboard.updateSetting('${moduleName}', '${setting.name}', this.checked)">`;
                } else if (setting.type === 'Number') {
                    inputHtml = `<input type="number" class="setting-input" value="${setting.value}" 
                                onchange="dashboard.updateSetting('${moduleName}', '${setting.name}', this.value)">`;
                } else {
                    inputHtml = `<input type="text" class="setting-input" value="${setting.value}" 
                                onchange="dashboard.updateSetting('${moduleName}', '${setting.name}', this.value)">`;
                }
                
                settingDiv.innerHTML = `
                    <label class="setting-label">${setting.name}</label>
                    ${inputHtml}
                `;
                
                settingsDiv.appendChild(settingDiv);
            });
        } catch (error) {
            console.error('Failed to load settings:', error);
        }
    }

    async toggleModule(name, enabled) {
        try {
            await fetch('/api/modules', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, enabled })
            });
            this.loadModules();
        } catch (error) {
            console.error('Failed to toggle module:', error);
        }
    }

    async updateSetting(module, setting, value) {
        try {
            await fetch('/api/settings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ module, setting, value: value.toString() })
            });
        } catch (error) {
            console.error('Failed to update setting:', error);
        }
    }

    async updateConsole() {
        try {
            const response = await fetch('/api/logs');
            const data = await response.json();
            
            const consoleOutput = document.getElementById('consoleOutput');
            consoleOutput.innerHTML = '';
            
            data.logs.forEach(log => {
                const logLine = document.createElement('div');
                logLine.className = 'console-line';
                logLine.textContent = log.message;
                consoleOutput.appendChild(logLine);
            });
            
            consoleOutput.scrollTop = consoleOutput.scrollHeight;
        } catch (error) {
            console.error('Failed to update console:', error);
        }
    }

    async executeCommand() {
        const input = document.getElementById('consoleInput');
        const command = input.value.trim();
        
        if (command) {
            try {
                await fetch('/api/command', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command })
                });
                input.value = '';
            } catch (error) {
                console.error('Failed to execute command:', error);
            }
        }
    }

    async sendMessage() {
        const input = document.getElementById('chatInput');
        const message = input.value.trim();
        
        if (message) {
            try {
                await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message })
                });
                input.value = '';
            } catch (error) {
                console.error('Failed to send message:', error);
            }
        }
    }

    async updateChat() {
        try {
            const response = await fetch('/api/chat');
            const data = await response.json();
            
            const chatMessages = document.getElementById('chatMessages');
            chatMessages.innerHTML = '';
            
            data.messages.forEach(msg => {
                const msgDiv = document.createElement('div');
                msgDiv.className = 'chat-message';
                msgDiv.textContent = msg.text;
                chatMessages.appendChild(msgDiv);
            });
            
            chatMessages.scrollTop = chatMessages.scrollHeight;
        } catch (error) {
            console.error('Failed to update chat:', error);
        }
    }

    async updatePlayers() {
        try {
            const response = await fetch('/api/players');
            const data = await response.json();
            
            const playersList = document.getElementById('playersList');
            playersList.innerHTML = '';
            
            data.players.forEach(player => {
                const playerDiv = document.createElement('div');
                playerDiv.className = 'player-item';
                playerDiv.innerHTML = `
                    <div><strong>${player.name}</strong></div>
                    <div>Health: ${player.health}</div>
                    <div>Distance: ${player.distance}m</div>
                    <div>Pos: ${player.x}, ${player.y}, ${player.z}</div>
                `;
                playersList.appendChild(playerDiv);
            });
            
            this.updateMinimap(data.players);
        } catch (error) {
            console.error('Failed to update players:', error);
        }
    }

    setupMinimap() {
        this.minimapCanvas = document.getElementById('minimap');
        this.minimapCtx = this.minimapCanvas.getContext('2d');
    }

    updateMinimap(players) {
        const ctx = this.minimapCtx;
        const canvas = this.minimapCanvas;
        
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        // Draw background
        ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        // Draw grid
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
        ctx.lineWidth = 1;
        for (let i = 0; i < canvas.width; i += 30) {
            ctx.beginPath();
            ctx.moveTo(i, 0);
            ctx.lineTo(i, canvas.height);
            ctx.stroke();
        }
        for (let i = 0; i < canvas.height; i += 30) {
            ctx.beginPath();
            ctx.moveTo(0, i);
            ctx.lineTo(canvas.width, i);
            ctx.stroke();
        }
        
        // Draw players
        players.forEach(player => {
            const x = (player.x % 300 + 300) % 300;
            const z = (player.z % 300 + 300) % 300;
            
            ctx.fillStyle = player.name === 'You' ? '#00ff00' : '#ff0000';
            ctx.beginPath();
            ctx.arc(x, z, 5, 0, 2 * Math.PI);
            ctx.fill();
            
            ctx.fillStyle = 'white';
            ctx.font = '10px Arial';
            ctx.fillText(player.name, x + 8, z + 3);
        });
    }

    async startMovement(action) {
        try {
            await fetch('/api/movement', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action, state: 'start' })
            });
        } catch (error) {
            console.error('Failed to start movement:', error);
        }
    }

    async stopMovement(action) {
        try {
            await fetch('/api/movement', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action, state: 'stop' })
            });
        } catch (error) {
            console.error('Failed to stop movement:', error);
        }
    }

    async takeScreenshot() {
        try {
            const response = await fetch('/api/screenshot', { method: 'POST' });
            const data = await response.json();
            if (data.success) {
                alert('Screenshot saved!');
            }
        } catch (error) {
            console.error('Failed to take screenshot:', error);
        }
    }

    async disconnectGame() {
        if (confirm('Disconnect from server?')) {
            try {
                await fetch('/api/disconnect', { method: 'POST' });
            } catch (error) {
                console.error('Failed to disconnect:', error);
            }
        }
    }

    async exitGame() {
        if (confirm('Exit game? This will close Minecraft.')) {
            try {
                await fetch('/api/exit', { method: 'POST' });
            } catch (error) {
                console.error('Failed to exit game:', error);
            }
        }
    }


}

// Initialize dashboard
const dashboard = new DebugDashboard();