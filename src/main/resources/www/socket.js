import { signal } from 'https://esm.sh/@preact/signals';

function readMessage(data) {
  const [_, type, id, payload] = data.match(/(\w+)(?:\[(\w+)\])?({.*})?/s)
  return [type, payload && JSON.parse(payload), id]
}

const socketProtocol = location.protocol === "https:" ? "wss:" : "ws:"

export class Socket {
  constructor(path) {
    this.path = path
    this.handlers = new Map()
    this.errorHandlers = new Map()
    this.state = signal(null)
    this.on("State", state => this.state.value = state)
    this.on("Id", ({ id }) => localStorage.setItem("id", id))
    this.on("GetId", (_, respond) => respond({ id: localStorage.getItem("id") }))
    this.on("GetName", (_, respond) => {
      const name = localStorage.getItem('name')
      name ? respond({ name }) : location.assign('/name.html')
    })
    this.on("Error", (error) => {
      error.message && console.error(error.message)
      this.errorHandlers.get(error.type)?.(error)
    })
    window.addEventListener("load", () => this.connect())
  }

  connect() {
    this.ws = new WebSocket(`${socketProtocol}${location.host}${this.path}${location.search}`)
    this.ws.onmessage = (msg) => {
      const [type, message, id] = readMessage(msg.data)
      const handler = this.handlers.get(type)
      if (!handler) {
        console.error(`No handler for type ${type}`)
        return
      }
      const respond = id ? (msg => this.send(`[${id}]`, msg)) : (msg => this.send("", msg))
      handler(message, respond)
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
    const frame = content ? messageType + JSON.stringify(content) : messageType
    this.ws.send(frame)
  }

  on(type, handler) {
    this.handlers.set(type, handler)
  }

  onError(type, handler) {
    this.errorHandlers.set(type, handler)
  }
}
