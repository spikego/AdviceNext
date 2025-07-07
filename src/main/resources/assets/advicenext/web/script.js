class DebugDashboard {
    constructor() {
        this.ws = null;
        this.modules = [];
        this.logs = [];
        this.isStreaming = false;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.connectWebSocket();
        this.loadModules();
        this.startPeriodicUpdates();
    }

    setupEventListeners() {
        document.getElementById('refresh-btn').addEventListener('click', () => {
            this.loadModules();
            this.loadLogs();
        });

        document.getElementById('send-command').addEventListener('click', () => {
            this.sendCommand();
        });

        document.getElementById('command-input').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendCommand();
            }
        });

        document.getElementById('start-stream').addEventListener('click', () => {
            this.startGameStream();
        });

        document.getElementById('stop-stream').addEventListener('click', () => {
            this.stopGameStream();
        });
    }

    connectWebSocket() {
        try {
            this.ws = new WebSocket(`ws://${window.location.host}/ws`);
            
            this.ws.onopen = () => {
                this.updateConnectionStatus('Connected', true);
            };

            this.ws.onmessage = (event) => {
                const data = JSON.parse(event.data);
                this.handleWebSocketMessage(data);
            };

            this.ws.onclose = () => {
                this.updateConnectionStatus('Disconnected', false);
                setTimeout(() => this.connectWebSocket(), 5000);
            };

            this.ws.onerror = () => {
                this.updateConnectionStatus('Error', false);
            };
        } catch (error) {
            console.error('WebSocket connection failed:', error);
            this.updateConnectionStatus('Failed', false);
        }
    }

    updateConnectionStatus(status, connected) {
        const statusElement = document.getElementById('connection-status');
        statusElement.textContent = status;
        statusElement.style.background = connected ? 
            'rgba(0, 255, 0, 0.2)' : 'rgba(255, 0, 0, 0.2)';
    }

    async loadModules() {
        try {
            const response = await fetch('/api/modules');
            const data = await response.json();
            this.modules = data.modules || [];
            this.renderModules();
        } catch (error) {
            console.error('Failed to load modules:', error);
        }
    }

    renderModules() {
        const container = document.getElementById('modules-list');
        container.innerHTML = '';

        this.modules.forEach(module => {
            const moduleElement = document.createElement('div');
            moduleElement.className = 'module-item';
            moduleElement.innerHTML = `
                <div class="module-info">
                    <span>${module.name}</span>
                    ${module.hasSettings ? '<button class="settings-btn" data-module="' + module.name + '">⚙️</button>' : ''}
                </div>
                <div class="module-toggle ${module.enabled ? 'active' : ''}" 
                     data-module="${module.name}">
                </div>
            `;

            const toggle = moduleElement.querySelector('.module-toggle');
            toggle.addEventListener('click', () => {
                this.toggleModule(module.name, !module.enabled);
            });
            
            const settingsBtn = moduleElement.querySelector('.settings-btn');
            if (settingsBtn) {
                settingsBtn.addEventListener('click', () => {
                    this.showModuleSettings(module.name);
                });
            }

            container.appendChild(moduleElement);
        });
    }

    async toggleModule(name, enabled) {
        try {
            const response = await fetch('/api/modules', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, enabled })
            });

            if (response.ok) {
                this.loadModules();
                this.addLog(`Module ${name} ${enabled ? 'enabled' : 'disabled'}`, 'info');
            }
        } catch (error) {
            console.error('Failed to toggle module:', error);
            this.addLog(`Failed to toggle module ${name}`, 'error');
        }
    }

    async loadLogs() {
        try {
            const response = await fetch('/api/logs');
            const data = await response.json();
            this.logs = data.logs || [];
            this.renderLogs();
        } catch (error) {
            console.error('Failed to load logs:', error);
        }
    }

    renderLogs() {
        const console = document.getElementById('console');
        console.innerHTML = '';

        this.logs.slice(-50).forEach(log => {
            this.addLogToConsole(log);
        });

        console.scrollTop = console.scrollHeight;
    }

    addLog(message, type = 'info') {
        const timestamp = new Date().toLocaleTimeString();
        const logEntry = `[${timestamp}] ${message}`;
        this.logs.push({ message: logEntry, type });
        this.addLogToConsole({ message: logEntry, type });
    }

    addLogToConsole(log) {
        const console = document.getElementById('console');
        const logElement = document.createElement('div');
        logElement.className = `log-entry ${log.type}`;
        logElement.textContent = log.message;
        console.appendChild(logElement);
        console.scrollTop = console.scrollHeight;
    }

    async sendCommand() {
        const input = document.getElementById('command-input');
        const command = input.value.trim();
        
        if (!command) return;

        try {
            const response = await fetch('/api/command', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ command })
            });

            const result = await response.json();
            this.addLog(`> ${command}`, 'info');
            this.addLog(result.output || 'Command executed', 'info');
            
            input.value = '';
        } catch (error) {
            this.addLog(`Command failed: ${error.message}`, 'error');
        }
    }

    startGameStream() {
        if (this.isStreaming) return;

        this.isStreaming = true;
        const canvas = document.getElementById('game-canvas');
        const ctx = canvas.getContext('2d');

        // 模拟游戏画面流
        this.streamInterval = setInterval(() => {
            this.renderGameFrame(ctx);
        }, 1000 / 30); // 30 FPS

        this.addLog('Game stream started', 'info');
    }

    stopGameStream() {
        if (!this.isStreaming) return;

        this.isStreaming = false;
        if (this.streamInterval) {
            clearInterval(this.streamInterval);
        }

        this.addLog('Game stream stopped', 'info');
    }

    renderGameFrame(ctx) {
        // 清除画布
        ctx.fillStyle = 'rgba(0, 0, 0, 0.1)';
        ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

        // 绘制模拟游戏内容
        ctx.fillStyle = '#00ff00';
        const time = Date.now() * 0.001;
        const x = Math.sin(time) * 100 + 200;
        const y = Math.cos(time) * 50 + 150;
        
        ctx.beginPath();
        ctx.arc(x, y, 10, 0, Math.PI * 2);
        ctx.fill();

        // 添加文本
        ctx.fillStyle = '#ffffff';
        ctx.font = '16px Arial';
        ctx.fillText('Game Stream Active', 10, 30);
        ctx.fillText(`Time: ${time.toFixed(1)}s`, 10, 50);
    }

    startPeriodicUpdates() {
        setInterval(() => {
            this.updateSystemInfo();
            this.loadLogs();
        }, 2000);
    }

    async updateSystemInfo() {
        try {
            const response = await fetch('/api/system');
            const data = await response.json();
            
            document.getElementById('fps').textContent = data.fps || '0';
            document.getElementById('memory').textContent = `${data.memory || 0} MB`;
            document.getElementById('position').textContent = 
                `${data.x || 0}, ${data.y || 0}, ${data.z || 0}`;
        } catch (error) {
            // 静默处理错误
        }
    }

    async showModuleSettings(moduleName) {
        try {
            const response = await fetch(`/api/settings?module=${moduleName}`);
            const data = await response.json();
            this.renderSettingsModal(moduleName, data.settings);
        } catch (error) {
            this.addLog(`Failed to load settings for ${moduleName}`, 'error');
        }
    }
    
    renderSettingsModal(moduleName, settings) {
        const modal = document.createElement('div');
        modal.className = 'settings-modal';
        modal.innerHTML = `
            <div class="modal-content glass-panel">
                <h3>${moduleName} Settings</h3>
                <div class="settings-list">
                    ${settings.map(setting => `
                        <div class="setting-item">
                            <label>${setting.name}</label>
                            <input type="text" value="${setting.value}" data-setting="${setting.name}">
                        </div>
                    `).join('')}
                </div>
                <div class="modal-buttons">
                    <button class="save-btn">Save</button>
                    <button class="cancel-btn">Cancel</button>
                </div>
            </div>
        `;
        
        modal.querySelector('.save-btn').addEventListener('click', () => {
            this.saveSettings(moduleName, modal);
        });
        
        modal.querySelector('.cancel-btn').addEventListener('click', () => {
            document.body.removeChild(modal);
        });
        
        document.body.appendChild(modal);
    }
    
    async saveSettings(moduleName, modal) {
        const inputs = modal.querySelectorAll('input');
        for (const input of inputs) {
            const settingName = input.dataset.setting;
            const value = input.value;
            
            try {
                await fetch('/api/settings', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ module: moduleName, setting: settingName, value })
                });
            } catch (error) {
                this.addLog(`Failed to save ${settingName}`, 'error');
            }
        }
        
        document.body.removeChild(modal);
        this.addLog(`Settings saved for ${moduleName}`, 'info');
    }

    handleWebSocketMessage(data) {
        switch (data.type) {
            case 'log':
                this.addLog(data.message, data.level);
                break;
            case 'module_update':
                this.loadModules();
                break;
            case 'system_info':
                this.updateSystemInfo();
                break;
        }
    }
}

// 初始化仪表板
document.addEventListener('DOMContentLoaded', () => {
    new DebugDashboard();
});