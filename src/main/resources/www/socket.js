import { signal } from "signals";

const socketProtocol = location.protocol === "https:" ? "wss:" : "ws:"

function readMessage(message) {
  let [_, type, content] = message.match(/^(\w+)(:?.*)$/)
  if (content?.[0] == ':') content = content.substring(1)
  if (content?.match(/^[[{]/)) content = JSON.parse(content)
  return [type, content]
}

class Socket {
  constructor(path) {
    this.path = path
    this.handlers = new Map()
    this.state = signal(null)
    this.prompts = signal([])
    this.connected = signal(false)
    this.on("State", state => this.state.value = state)
    this.on("Prompts", prompts => this.prompts.value = 
      prompts.map((prompt) => ({...prompt, respond: (msg) => this.send(`[${prompt.id}]` + JSON.stringify(msg))}))
    )
    this.on("Id", (id) => localStorage.setItem("id", id))
    this.on("GetId", (_, respond) => respond(JSON.stringify({ id: localStorage.getItem("id") })))
    this.on("GetName", (_, respond) => {
      const name = localStorage.getItem('name')
      name ? respond(JSON.stringify({ name })) :
        location.assign(`/name.html?redirect=` + encodeURIComponent(location.pathname + location.search))
    })
    window.addEventListener("load", () => this.connect())
  }

  connect() {
    this.ws = new WebSocket(`${socketProtocol}${location.host}${this.path}${location.search}`)
    this.ws.onmessage = (msg) => {
      this.connected.value = true
      const [type, message] = readMessage(msg.data)
      const handler = this.handlers.get(type)
      if (!handler) {
        console.error(`No handler for type ${type}`)
        return
      }
      handler(message, response => this.send(response))
    }
    this.ws.onclose = () => this.reconnect()
  }

  reconnect() {
    this.connected.value = false
    setTimeout(() => {
      if (!this.backoff) this.backoff = 100;
      else if (this.backoff < 1000) this.backoff *= 10;
      this.connect();
    }, this.backoff)
  }

  send(frame) {
    this.ws.send(frame)
  }

  on(type, handler) {
    this.handlers.set(type, handler)
  }
}

export function newSocket(path) {
  return new Socket(path)
}