import { signal } from "signals";

function readMessage(data) {
  const [_, type, payload] = data.match(/(\w+)(.*)/s)
  return [type, payload && JSON.parse(payload)]
}

const socketProtocol = location.protocol === "https:" ? "wss:" : "ws:"

class Socket {
  constructor(path) {
    this.path = path
    this.handlers = new Map()
    this.errorHandlers = new Map()
    this.state = signal(null)
    this.prompts = signal([])
    this.on("State", state => this.state.value = state)
    this.on("Id", ({ id }) => localStorage.setItem("id", id))
    this.on("GetId", (_, respond) => respond({ id: localStorage.getItem("id") }))
    this.on("GetName", (_, respond) => {
      const name = localStorage.getItem('name')
      name ? respond({ name }) :
        location.assign(`/name.html?redirect=` + encodeURIComponent(location.pathname + location.search))
    })
    this.on("Error", (error) => {
      this.errorHandlers.get(error.type)?.(error)
    })
    window.addEventListener("load", () => this.connect())
  }

  connect() {
    this.ws = new WebSocket(`${socketProtocol}${location.host}${this.path}${location.search}`)
    this.ws.onmessage = (msg) => {
      const [type, message] = readMessage(msg.data)
      if (type === "Prompts") {
        this.prompts.value = message.map(({type, id, prompt}) => ({type, message: prompt, respond: (msg => this.send(`[${id}]`, msg))}))
      } else {
        const handler = this.handlers.get(type)
        if (!handler) {
          console.error(`No handler for type ${type}`)
          return
        }
        const respond = msg => this.send("", msg)
        handler(message, respond)
      }
    }
    this.ws.onclose = () => this.reconnect()
  }

  reconnect() {
    setTimeout(() => {
      if (!this.backoff) this.backoff = 100;
      else if (this.backoff < 1000) this.backoff *= 10;
      this.connect();
    }, this.backoff)
  }

  send(messageType, content) {
    const frame = messageType + (content ? JSON.stringify(content) : "")
    this.ws.send(frame)
  }

  on(type, handler) {
    this.handlers.set(type, handler)
  }

  onError(type, handler) {
    this.errorHandlers.set(type, handler)
  }
}

export function newSocket(path) {
  return new Socket(path)
}