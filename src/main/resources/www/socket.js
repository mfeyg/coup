function readMessage(data) {
  const [_, type, id, payload] = data.match(/(\w+)(?:\[(\w+)\])?({.*})?/s)
  return [type, payload && JSON.parse(payload), id]
}

const protocol = location.protocol === "https:" ? "wss:" : "ws:"

export class Socket {
  constructor(path) {
    this.ws = new WebSocket(`${protocol}${location.host}${path}${location.search}`)
    this.handlers = new Map()
    this.errorHandlers = new Map()
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
    this.on("Id", ({ id }) => localStorage.setItem("id", id))
    this.on("GetId", (_, respond) => respond({ id: localStorage.getItem("id") }))
    this.on("GetName", (_, respond) => {
      const name = localStorage.getItem('name')
      name ? respond({name}) : location.replace('/')
    })
    this.on("Error", (error) => {
      console.error(error.message)
      this.errorHandlers.get(error.type)?.(error) 
    })
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
