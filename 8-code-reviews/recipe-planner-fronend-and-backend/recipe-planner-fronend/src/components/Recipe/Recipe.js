import './Recipe.css'
import {Button, Card } from "react-bootstrap";
import {Link} from "react-router-dom";
import React from "react";
function Recipe(props) {
    return (
    <Card style={{ width: '18rem' }}>
        <Card.Img variant="top" src={props.image} />
        <Card.Body>
            <Card.Title>{props.title}</Card.Title>
            {props.description}
            <Button variant="primary" as={Link} to={`/recipes/${props.id}/edit`}>Edit Details</Button>
        </Card.Body>
    </Card>
    )
}

export default Recipe;
