import React from 'react';
import './AddIngredient.css';
import {Button, Col, Form, Row} from "react-bootstrap";

// Mirrors the Unit enum of the backend
const UNITS = ["PIECE", "GRAMM", "KILOGRAMM", "LITRE", "DECILITRE"];

const AddIngredient = ({ingredient, updateIngredient, removeIngredient}) => {

    const updateField = ({target}) => {
        updateIngredient({...ingredient, [target.name]: target.value})
    }

    return (
        <Row>
            <Col>
                <Form.Group className="mb-1">
                    <Form.Control
                        name="name"
                        placeholder="Name"
                        value={ingredient.name}
                        onChange={updateField}
                        required
                    />
                </Form.Group>
            </Col>
            <Col>
                <Form.Group className="mb-1">
                    <Form.Select name="unit" value={ingredient.unit} onChange={updateField}>
                        {UNITS.map(unit => <option key={unit} value={unit}>{unit}</option>)}
                    </Form.Select>
                </Form.Group>
            </Col>
            <Col>
                <Form.Group className="mb-1">
                    <Form.Control
                        name="amount"
                        type="number"
                        min="0"
                        placeholder="Quantity"
                        value={ingredient.amount}
                        onChange={updateField}
                        required
                    />
                </Form.Group>
            </Col>
            <Col xs={1}>
                <Button
                    onClick={() => removeIngredient(ingredient)}
                    variant='outline-dark'
                    className="mb-1"
                >x</Button>
            </Col>
        </Row>
    )
}

export default AddIngredient;
