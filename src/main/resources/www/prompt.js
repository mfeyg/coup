import { html } from "htm/preact"
import { useState } from "preact/hooks"
import { Dialog } from "./dialog.js"

function Timer({children}) {
  if (children != null) {
    return `(${children})`
  }
}

function ActionDescription({ action, player }) {
  switch (action.type) {
    case "Foreign Aid":
      return `${action.player.name} wishes to receive foreign aid.`
    case "Tax":
      return `${action.player.name} wishes to collect taxes.`
    case "Steal":
      return `${action.player.name} wishes to steal from ${action.target.number === player.number ? "you" : action.target.name}.`
    case "Exchange":
      return `${action.player.name} wishes to exchange with the deck.`
    case "Assassinate":
      return `${action.player.name} wishes to assassinate ${action.target.number === player.number ? "you" : action.target.name}.`
  }
}

const handlers = new Map()

handlers.set("TakeTurn", function TakeTurn({ prompt, respond, timer }) {
  const [action, setAction] = useState(null)

  if (action != null) {
    return html`<${Dialog}
     message=${html`Choose a target <${Timer}>${timer}</${Timer}>`}
     buttons=${action.targets.map(target => ({
        label: target.name, onSelect() { 
          respond({ actionType: action.actionType, target: target.number })
        } }))} />`
  }

  return html`<${Dialog} 
    message=${html`Choose an action <${Timer}>${timer}</${Timer}>`} 
    buttons=${prompt.options.map(action => ({
      label: action.actionType, onSelect() {
        if (action.targets?.length) {
          setAction(action)
          return
        }
        respond({actionType: action.actionType})
      }
  }))} />`
})


handlers.set("RespondToAction", function RespondToAction({ prompt, player, respond, timer }) {
  const [blocking, setBlocking] = useState(false)
  if (blocking) {
    const blockAs = (influence) => respond({reaction: "Block", influence: influence})
    return html`<${Dialog}
       message=${html`Choose an influence to block as: <${Timer}>${timer}</${Timer}>`}
       buttons=${[
        ...prompt.action.blockingInfluences.map(influence =>
         ({label: influence, onSelect() { blockAs(influence) }})
        ), {label: "Never mind", onSelect() { setBlocking(false) }}
      ]}
      />`
  }
  const buttons = []
  const allowButton = {label: "Allow", onSelect() { respond({reaction: "Allow"})}}
  const blockButton = {label: "Block", onSelect() { setBlocking(true) }}
  const challengeButton = {
    label: `Challenge claim of ${prompt.action.claimedInfluence}`,
    onSelect() { respond({reaction: "Challenge"}) }
  }
  buttons.push(allowButton)
  prompt.action.canBeBlocked && buttons.push(blockButton)
  prompt.action.canBeChallenged && buttons.push(challengeButton)
  return html`<${Dialog} 
    message=${html`
      <${ActionDescription} player=${player} action=${prompt.action} />
      <p>How will you respond? <${Timer}>${timer}</${Timer}></p>`} 
    buttons=${buttons}
    />`
})

handlers.set("RespondToBlock", function RespondToBlock({ prompt, respond, timer }) {
  return html`<${Dialog}
     message=${html`
      <p>${prompt.blocker.name} wishes to block as the ${prompt.influence}</p>
      <p>How will you respond? <${Timer}>${timer}</${Timer}></p>`}
     buttons=${[
      {label: "Allow", onSelect() { respond({reaction: "Allow"})}},
      {label: "Challenge", onSelect() { respond({reaction: "Challenge"})}},
     ]} />`
})

handlers.set("RespondToChallenge", function RespondToChallenge({ prompt, player, respond, timer }) {
  return html`<${Dialog}
    message=${html`
      <p>${prompt.challenger.name} challenges your claim of ${prompt.claim}</p>
      <p>Select an influence to reveal <${Timer}>${timer}</${Timer}></p>`}
    buttons=${player.heldInfluences.map(influence => ({
      label: influence,
      onSelect() { respond({influence}) },
    }))} />`
})

handlers.set("SurrenderInfluence", function SurrenderInfluence({ player, respond, timer }) {
  return html`<${Dialog}
    message=${html`Choose an influence to surrender <${Timer}>${timer}</${Timer}>`} 
    buttons=${player.heldInfluences.map(influence => ({
      label: influence,
      onSelect() { respond({influence}) },
    }))} />`
})

handlers.set("Exchange", function Exchange({ prompt, player, respond, timer }) {
  const influences = player.heldInfluences.concat(prompt.drawnInfluences)
  const [selected, setSelected] = useState([])
  const leftToPick = player.heldInfluences.length - selected.length
  const remaining = influences
  selected.forEach(influence =>
    remaining.splice(remaining.indexOf(influence), 1)
  )
  if (leftToPick === 0) {
    respond({returnedInfluences: remaining})
  }
  return html`<${Dialog}
  message=${html`
    <p>You drew: ${prompt.drawnInfluences.join(", ")}</p>
    <p>Select influence(s) to keep <${Timer}>${timer}</${Timer}></p>`}
  buttons=${remaining.map(influence => ({
    label: influence,
    onSelect() { setSelected(selected.concat(influence)) },
  }))} />`
})

export function Prompt({ prompt, player }) {
  const respond = (response) => {
    prompt.respond(response)
  }

  if (handlers.has(prompt?.type)) {
    return html`<${handlers.get(prompt.type)} 
      player=${player}
      prompt=${prompt.prompt}
      respond=${respond}
      timer=${prompt.timeout}
    />`
  }
}